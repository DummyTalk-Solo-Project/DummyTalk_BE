<#
.SYNOPSIS
  K6 stage matrix runner - runs the full VU matrix for a stage, then builds a markdown summary table.

.DESCRIPTION
  Runs k6/dummy-spike-test.js once per (USERS x CONCURRENT) combo with a cooldown between runs.
  Each run writes a compact JSON via handleSummary() to k6/results/.
  After all runs, aggregates JSONs into k6/results/stageN-summary.md.

  NOTE (ASCII only): this file intentionally avoids Korean text because
  Windows PowerShell 5.1 misreads BOM-less UTF-8 script files.

.EXAMPLE
  # Stage 3 full matrix against EC2
  .\k6\run-stage-matrix.ps1 -Stage 3 -BaseUrl "http://<EC2_IP>"

  # Stage 4 matrix (VU 100/200/500/2000) against local
  .\k6\run-stage-matrix.ps1 -Stage 4 -BaseUrl "http://localhost:8080" -CooldownSec 30

  # Re-run only specific total-VU combos
  .\k6\run-stage-matrix.ps1 -Stage 3 -BaseUrl "http://<EC2_IP>" -Only 500,800

  # Rebuild the summary table from existing JSONs without running k6
  .\k6\run-stage-matrix.ps1 -Stage 3 -TableOnly
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('1', '2', '3', '4')]
    [string]$Stage,

    [string]$BaseUrl = 'http://localhost:8080',

    # Cooldown between runs: lets Tomcat queue drain and T3 CPU credits recover a bit
    [int]$CooldownSec = 120,

    # Filter: run only these total-VU values (e.g. -Only 500,800)
    [int[]]$Only,

    # Skip k6 execution, only rebuild the markdown table from existing JSONs
    [switch]$TableOnly
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
    Write-Error "k6 not found in PATH. Install: winget install k6"
}

# ---- Test matrices ----------------------------------------------------------
# Stage 1~3: same 8 combos used in Stage 1/2 (comparable graphs)
# Stage 4:   reduced matrix VU = 100 / 200 / 500 / 2000 (CONCURRENT fixed at 5)
$matrices = @{
    '1' = @(
        @{ u = 45;  c = 4 }, @{ u = 40;  c = 5 }, @{ u = 35;  c = 6 }, @{ u = 50;  c = 5 },
        @{ u = 60;  c = 5 }, @{ u = 80;  c = 5 }, @{ u = 100; c = 5 }, @{ u = 100; c = 8 }
    )
    '2' = @(
        @{ u = 45;  c = 4 }, @{ u = 40;  c = 5 }, @{ u = 35;  c = 6 }, @{ u = 50;  c = 5 },
        @{ u = 60;  c = 5 }, @{ u = 80;  c = 5 }, @{ u = 100; c = 5 }, @{ u = 100; c = 8 }
    )
    '3' = @(
        @{ u = 45;  c = 4 }, @{ u = 40;  c = 5 }, @{ u = 35;  c = 6 }, @{ u = 50;  c = 5 },
        @{ u = 60;  c = 5 }, @{ u = 80;  c = 5 }, @{ u = 100; c = 5 }, @{ u = 100; c = 8 }
    )
    '4' = @(
        @{ u = 20;  c = 5 }, @{ u = 40;  c = 5 }, @{ u = 100; c = 5 }, @{ u = 400; c = 5 }
    )
}

$runs = $matrices[$Stage]
if ($Only) {
    $runs = @($runs | Where-Object { $Only -contains ($_.u * $_.c) })
    if ($runs.Count -eq 0) { Write-Error "No matrix combo matches -Only $($Only -join ',')" }
}

$stageTag = "stage$Stage"
$resultsDir = Join-Path $repoRoot 'k6\results'
if (-not (Test-Path $resultsDir)) { New-Item -ItemType Directory -Path $resultsDir | Out-Null }

# ---- Run k6 -----------------------------------------------------------------
if (-not $TableOnly) {
    $maxUsers = ($runs | ForEach-Object { $_.u } | Measure-Object -Maximum).Maximum
    Write-Host ""
    Write-Host "== $stageTag matrix: $($runs.Count) runs, target=$BaseUrl, cooldown=${CooldownSec}s ==" -ForegroundColor Cyan
    Write-Host "   Requires test accounts test2..test$($maxUsers + 1) seeded on the target." -ForegroundColor DarkYellow
    Write-Host ""

    $i = 0
    foreach ($r in $runs) {
        $i++
        $vu = $r.u * $r.c
        Write-Host "--- [$i/$($runs.Count)] $stageTag VU=$vu (USERS=$($r.u) x CONCURRENT=$($r.c)) ---" -ForegroundColor Cyan

        k6 run `
            -e BASE_URL=$BaseUrl `
            -e USERS=$($r.u) `
            -e CONCURRENT=$($r.c) `
            -e STAGE=$stageTag `
            k6/dummy-spike-test.js

        # exit 99 = threshold failed (still a valid measurement) -> continue
        # any other non-zero = script/runtime error -> warn but keep going so one bad run doesn't kill the batch
        if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne 99) {
            Write-Warning "k6 exited with code $LASTEXITCODE on VU=$vu (runtime error?). Check output above."
        }

        if ($i -lt $runs.Count) {
            Write-Host "cooldown ${CooldownSec}s ..." -ForegroundColor DarkGray
            Start-Sleep -Seconds $CooldownSec
        }
    }
}

# ---- Aggregate JSONs into markdown table ------------------------------------
$files = Get-ChildItem (Join-Path $resultsDir "$stageTag-vu*.json") -ErrorAction SilentlyContinue
if (-not $files) {
    Write-Warning "No result JSONs found for $stageTag. Nothing to aggregate."
    return
}

$rows = foreach ($f in $files) { Get-Content $f.FullName -Raw | ConvertFrom-Json }
$rows = $rows | Sort-Object total_vu, users

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# $stageTag results ($(Get-Date -Format 'yyyy-MM-dd HH:mm'))")
$md.Add("")
$md.Add("| VU | Combo | p50 | p95 | p99 | max | reject p95 | 200 OK | 429 | 429 rate | limit | race | VU check | http_failed | RPS | p95<5s |")
$md.Add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")

foreach ($r in $rows) {
    $eqMark   = if ($r.vu_equation_ok) { "O ($($r.vu_equation_sum))" } else { "X ($($r.vu_equation_sum)/$($r.total_vu))" }
    $p95Mark  = if ($r.p95 -lt 5000) { "O" } else { "X" }
    $ffRate   = "{0:P1}" -f $r.fast_fail_rate
    $failRate = "{0:P1}" -f $r.http_req_failed_rate
    $md.Add("| $($r.total_vu) | $($r.users)x$($r.concurrent) | $([math]::Round($r.p50)) | $([math]::Round($r.p95)) | $([math]::Round($r.p99)) | $([math]::Round($r.max)) | $([math]::Round($r.reject_p95)) | $($r.success_200) | $($r.fast_fail_429) | $ffRate | $($r.limit_hit) | $($r.race_suspect) | $eqMark | $failRate | $([math]::Round($r.http_reqs_per_sec, 1)) | $p95Mark |")
}

$md.Add("")
$md.Add("- VU check: total VU = 200-success + 429-reject + limit-hit. X means some requests got an unexpected status (timeout / 5xx).")
$md.Add("- reject p95: latency of the 429 (rejection) path only. Compare stage2(lock) vs stage3(interceptor) here.")
$md.Add("- DB ground-truth check (run manually, Lost Update proof):")
$md.Add('  ```sql')
$md.Add("  SELECT SUM(i.req_count) FROM info i;  -- delta since last reset must equal SUM(success_200)")
$md.Add('  ```')

$outFile = Join-Path $resultsDir "$stageTag-summary.md"
$md -join "`n" | Out-File $outFile -Encoding utf8

Write-Host ""
Write-Host "== Summary written: $outFile ==" -ForegroundColor Green
Write-Host ""
foreach ($line in $md) { Write-Host $line }
