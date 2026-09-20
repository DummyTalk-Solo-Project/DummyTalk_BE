<#
.SYNOPSIS
  K6 single-run wrapper - runs ONE VU config, saves timestamped JSON/TXT, rebuilds the
  stage summary table (latest-per-VU) and the all-stages run-history log.

.DESCRIPTION
  One invocation = one k6 run (so you can reset DB fields between runs).
  Each run writes k6/results/<yyyyMMdd-HHmmss>_stageN-vu<VU>-<USERS>x<CONCURRENT>.json/.txt
  via handleSummary() in dummy-spike-test.js. Old files are never overwritten - every
  run keeps its own timestamped file, so re-running the same VU/stage combo (e.g. before
  vs after a code fix) no longer clobbers the earlier result.

  This script then rebuilds two things:
    1. k6/results/stageN-summary.md   - one row per VU for THIS stage tag, latest run wins
                                         (for charts: clean VU-indexed table)
    2. k6/results/run-history.md      - every run, every stage, every timestamp, one row
                                         each, newest first (for navigating "what did I run
                                         and when" across the whole results folder)

  NOTE (ASCII only): this file intentionally avoids Korean text because
  Windows PowerShell 5.1 misreads BOM-less UTF-8 script files.

.EXAMPLE
  # One run: total VU=1000 (USERS=200 x CONCURRENT=5)
  .\k6\run-stage-matrix.ps1 -Stage 3 -BaseUrl "https://ddotg.dev" -Vu 1000

  # Different concurrency per user
  .\k6\run-stage-matrix.ps1 -Stage 3 -BaseUrl "https://ddotg.dev" -Vu 800 -Concurrent 8

  # CP sweep run (Stage is a free-form tag, e.g. "4-cp20" -> files ..._stage4-cp20-vu*.json)
  .\k6\run-stage-matrix.ps1 -Stage "4-cp20" -BaseUrl "https://ddotg.dev" -Vu 800

  # Rebuild stageN-summary.md + run-history.md only (no k6 run)
  .\k6\run-stage-matrix.ps1 -Stage 3 -TableOnly
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Stage,

    # Total VU for this single run. USERS is derived as Vu / Concurrent.
    [int]$Vu,

    # Duplicate requests per user (dda-dak factor). Total VU must be divisible by this.
    [int]$Concurrent = 5,

    [string]$BaseUrl = 'http://localhost:8080',

    # Skip k6 execution, only rebuild the markdown tables from existing JSONs
    [switch]$TableOnly
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$stageTag = "stage$Stage"
$resultsDir = Join-Path $repoRoot 'k6\results'
if (-not (Test-Path $resultsDir)) { New-Item -ItemType Directory -Path $resultsDir | Out-Null }

# ---- Single k6 run ----------------------------------------------------------
if (-not $TableOnly) {
    if (-not $Vu -or $Vu -le 0) {
        Write-Error "Pass -Vu <totalVU> (e.g. -Vu 1000), or use -TableOnly to rebuild the tables."
    }
    if ($Vu % $Concurrent -ne 0) {
        Write-Error "Vu ($Vu) must be divisible by Concurrent ($Concurrent)."
    }
    if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
        Write-Error "k6 not found in PATH. Install: winget install k6"
    }

    $users = $Vu / $Concurrent

    Write-Host ""
    Write-Host "== $stageTag single run: VU=$Vu (USERS=$users x CONCURRENT=$Concurrent) -> $BaseUrl ==" -ForegroundColor Cyan
    Write-Host "   Test accounts needed: test2..test$($users + 1)" -ForegroundColor DarkYellow
    Write-Host "   If you want per-run DB ground truth, reset first:  UPDATE info SET req_count = 0;" -ForegroundColor DarkYellow
    Write-Host ""

    k6 run `
        -e BASE_URL=$BaseUrl `
        -e USERS=$users `
        -e CONCURRENT=$Concurrent `
        -e STAGE=$stageTag `
        k6/dummy-spike-test.js

    # exit 99 = threshold failed (still a valid measurement)
    if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne 99) {
        Write-Warning "k6 exited with code $LASTEXITCODE (runtime error?). Check output above."
    }
}

# ---- 1. Per-stage summary: latest-per-VU table ------------------------------
# Filenames are now <timestamp>_<stageTag>-vu...json, so match on "*_<stageTag>-vu*.json"
$stageFiles = Get-ChildItem (Join-Path $resultsDir "*_$stageTag-vu*.json") -ErrorAction SilentlyContinue
if (-not $stageFiles) {
    Write-Warning "No result JSONs found for $stageTag. Nothing to aggregate."
} else {
    $stageRows = foreach ($f in $stageFiles) {
        $r = Get-Content $f.FullName -Raw | ConvertFrom-Json
        $r | Add-Member -NotePropertyName file -NotePropertyValue $f.Name -PassThru
    }

    # Same VU/combo may have multiple timestamped runs (reruns) - keep only the latest by ran_at
    $latestRows = $stageRows |
        Group-Object total_vu, users, concurrent |
        ForEach-Object { $_.Group | Sort-Object ran_at -Descending | Select-Object -First 1 } |
        Sort-Object total_vu, users

    $md = New-Object System.Collections.Generic.List[string]
    $md.Add("# $stageTag results ($(Get-Date -Format 'yyyy-MM-dd HH:mm'))")
    $md.Add("")
    $md.Add("One row per VU - latest run wins if a VU/combo was measured more than once. See run-history.md for every individual run.")
    $md.Add("")
    $md.Add("| VU | Combo | p50 | p95 | p99 | max | reject p95 | 200 OK | 429 | 429 rate | limit | race | VU check | http_failed | RPS | p95<5s | file |")
    $md.Add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")

    foreach ($r in $latestRows) {
        $eqMark   = if ($r.vu_equation_ok) { "O ($($r.vu_equation_sum))" } else { "X ($($r.vu_equation_sum)/$($r.total_vu))" }
        $p95Mark  = if ($r.p95 -lt 5000) { "O" } else { "X" }
        $ffRate   = "{0:P1}" -f $r.fast_fail_rate
        $failRate = "{0:P1}" -f $r.http_req_failed_rate
        $md.Add("| $($r.total_vu) | $($r.users)x$($r.concurrent) | $([math]::Round($r.p50)) | $([math]::Round($r.p95)) | $([math]::Round($r.p99)) | $([math]::Round($r.max)) | $([math]::Round($r.reject_p95)) | $($r.success_200) | $($r.fast_fail_429) | $ffRate | $($r.limit_hit) | $($r.race_suspect) | $eqMark | $failRate | $([math]::Round($r.http_reqs_per_sec, 1)) | $p95Mark | $($r.file) |")
    }

    $md.Add("")
    $md.Add("- VU check: total VU = 200-success + 429-reject + limit-hit. X means some requests got an unexpected status (timeout / 5xx).")
    $md.Add("- reject p95: client-side latency of the 429 path (includes Tomcat queue wait + TLS, not just server CPU).")
    $md.Add("- DB ground-truth check (run manually, Lost Update proof):")
    $md.Add('  ```sql')
    $md.Add("  SELECT SUM(i.req_count) FROM info i;  -- delta since last reset must equal 200-success")
    $md.Add('  ```')

    $stageOutFile = Join-Path $resultsDir "$stageTag-summary.md"
    $md -join "`n" | Out-File $stageOutFile -Encoding utf8

    Write-Host ""
    Write-Host "== Stage summary written: $stageOutFile ==" -ForegroundColor Green
    Write-Host ""
    foreach ($line in $md) { Write-Host $line }
}

# ---- 2. Consolidated run-history: every run, every stage, newest first -----
$allFiles = Get-ChildItem (Join-Path $resultsDir "*.json") -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match '^\d{8}-\d{6}_' }

if ($allFiles) {
    $allRows = foreach ($f in $allFiles) {
        $r = Get-Content $f.FullName -Raw | ConvertFrom-Json
        $r | Add-Member -NotePropertyName file -NotePropertyValue $f.Name -PassThru
    }
    $allRows = $allRows | Sort-Object ran_at -Descending

    $hist = New-Object System.Collections.Generic.List[string]
    $hist.Add("# K6 run history (all stages, updated $(Get-Date -Format 'yyyy-MM-dd HH:mm'))")
    $hist.Add("")
    $hist.Add("Every run ever recorded in k6/results, newest first. Use this to find a specific run when file names get confusing.")
    $hist.Add("")
    $hist.Add("| ran_at | stage | VU | combo | p95 | 429 rate | file |")
    $hist.Add("| --- | --- | --- | --- | --- | --- | --- |")
    foreach ($r in $allRows) {
        $ffRate = "{0:P1}" -f $r.fast_fail_rate
        $hist.Add("| $($r.ran_at) | $($r.stage) | $($r.total_vu) | $($r.users)x$($r.concurrent) | $([math]::Round($r.p95)) | $ffRate | $($r.file) |")
    }

    $histOutFile = Join-Path $resultsDir "run-history.md"
    $hist -join "`n" | Out-File $histOutFile -Encoding utf8

    Write-Host ""
    Write-Host "== Run history written: $histOutFile ($($allRows.Count) runs) ==" -ForegroundColor Green
}
