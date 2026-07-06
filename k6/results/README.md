# K6 결과 파일 필드 설명

`dummy-spike-test.js`의 `handleSummary()`가 회차마다 자동 생성하는 파일들의 해석 가이드.

## 파일 구성

| 파일 | 내용 | 용도 |
| --- | --- | --- |
| `stageN-vu<VU>-<USERS>x<CONC>.json` | 압축 요약 (아래 필드) | 표·그래프 생성, 회차 간 비교 |
| `stageN-vu<VU>-<USERS>x<CONC>.txt` | K6 터미널 요약 원본 | `http_req_*` 세부 분해 진단용 |
| `stageN-summary.md` | 해당 스테이지 전체 회차 표 | `run-stage-matrix.ps1`이 자동 재생성 |

같은 조합을 재실행하면 **같은 파일명으로 덮어씀** → 보존할 결과는 이름을 바꿔둘 것 (예: `stage3-ttl5s-vu1000-...json`).

## JSON 필드

### 식별 정보

| 필드 | 의미 |
| --- | --- |
| `stage` | `-e STAGE=stage3`로 주입된 태그. 직접 k6 실행 시 누락되면 `stageX`가 됨 → 스크립트(`run-stage-matrix.ps1`) 사용 권장 |
| `users` | 테스트 유저 수 (test2 ~ test{users+1} 계정 사용) |
| `concurrent` | 유저 1명당 동시 따닥 요청 수 |
| `total_vu` | users × concurrent = 총 발사 요청 수 |
| `ran_at` | 실행 완료 시각 — **UTC 기준** (KST = +9h) |

### 레이턴시 (avg / p50 / p90 / p95 / p99 / max) — 단위 ms

**전부 커스텀 Trend `dummy_req_duration_ms`의 통계.** 처리량이 아니라 "뽑기 요청 1건의 응답 시간" 분포임.

- 측정 방법: 스크립트에서 `Date.now()`로 `http.get()` 전후를 감싼 값
- **K6 내장 `http_req_duration`과 다름**: 내장 지표는 "요청 전송 → 응답 수신"만 재지만,
  이 커스텀 지표는 **DNS 조회 + TCP 연결 + TLS 핸드셰이크까지 포함** (스파이크 시 VU마다 새 연결을 맺으므로 이 비용이 큼)
- 관계식: `dummy_req_duration_ms ≈ http_req_blocked + http_req_connecting + tls_handshaking + http_req_duration`
  (세부 분해는 같은 이름의 `.txt` 파일에서 확인)
- 백분위 해석: p50 = 중앙값(절반이 이보다 빠름), p95 = 95%가 이보다 빠름 (threshold 기준: p95 < 5000ms), p99 = 최악 1% 경계
- 이 값에는 **Tomcat 큐 대기 시간이 포함**됨 → 고VU에서 값이 커지는 주원인은 서버 처리가 아니라 큐 대기

### 거절 비용 (reject_p95 / reject_p99) — 단위 ms

**429 응답만 골라낸** `reject_429_duration_ms` Trend의 백분위.

- 목적: "빠른 거절이 실제로 빠른가?" 측정. Stage 2(분산락, AOP 진입 후 거절) vs Stage 3(인터셉터, DispatcherServlet 직후 거절)의 거절 위치 차이 비교
- 주의: **클라이언트 관점 값**이라 Tomcat 큐 대기 + TLS가 포함됨. 서버측 거절 로직 자체는 sub-ms여도
  큐가 길면 이 값이 커짐 (실측: 전 구간에서 전체 p95 대비 5~8%만 빠름 → 레이턴시 지배 요인 = 큐)

### 결과 분류 카운트

| 필드 | 의미 |
| --- | --- |
| `success_200` | HTTP 200 (뽑기 성공) 수 — DB `req_count` 증가와 1:1 대응해야 함 |
| `fast_fail_429` | HTTP 429 (따닥 차단) 수 — Stage 2: 분산락 CANT_GET_LOCK / Stage 3: 인터셉터 DUPLICATE_REQUEST |
| `fast_fail_rate` | 429 / total_vu. **이론값 = (concurrent−1)/concurrent** (예: 5 → 80%, 8 → 87.5%). 실측이 낮은 원인 두 가지: ① TTL/leaseTime 만료 유출(버그), ② 첫 요청 완료 후 키 해제 → 늦게 도착한 형제 요청 정당 통과(설계상 정상) |
| `limit_hit` | 하루 20회 소진(400 DUMMY_4001) 수 — DB 리셋 없이 연속 실행하면 나타남 |
| `race_suspect` | remainingCount < 0 또는 > 20 감지 수 — **0이어야 정상** (Stage 1에서만 발생 예상) |

### 검증식 (vu_equation_ok / vu_equation_sum)

```
total_vu = success_200 + fast_fail_429 + limit_hit
```

- `true` = 모든 요청이 성공/차단/한도소진 중 하나로 완전 분류됨 (타임아웃·5xx 없음)
- `false` = 차이만큼 예상 밖 응답 발생 → `.txt`의 `http_req_failed`와 K6 콘솔 에러 확인
- **Lost Update 최종 증명은 DB 대조가 필요**: 리셋 후 `SELECT SUM(req_count) FROM info;` == success_200 이어야 함
  (K6는 응답만 보므로 "200을 받았는데 DB에 안 써진" 케이스는 DB 대조로만 검출 가능)

### 처리량 (http_reqs_per_sec)

**주의 — 이 값은 근사치로만 쓸 것.** K6의 `http_reqs` rate인데 두 가지 오염이 있음:

1. **setup()의 로그인 요청도 포함** (VU=1000이면 로그인 200건 + 뽑기 1000건 = 1200건 기준)
2. **분모가 전체 실행 시간** (로그인 ~수십 초 + 스파이크 수 초) → 스파이크 순간 처리율보다 훨씬 낮게 나옴

스파이크 순간 처리율이 필요하면 `total_vu / (p99 ÷ 1000)` 로 근사하거나, Prometheus
`rate(http_server_requests_seconds_count[10s])`로 서버측 실측을 쓸 것.
회차 간 상대 비교 용도로는 setup 비중이 비슷하므로 참고 가능.

## .txt (K6 터미널 요약)에서 추가로 볼 수 있는 것

| 지표 | 용도 |
| --- | --- |
| `http_req_blocked` / `http_req_connecting` / `http_req_tls_handshaking` | 연결 수립 비용 — 커스텀 레이턴시와 내장 레이턴시의 차이 원인 |
| `http_req_waiting` | TTFB — 서버가 응답 시작까지 걸린 시간 (큐 대기 + 처리) |
| `http_req_failed` | 예상 외 상태코드 비율 (200/400/429는 expectedStatuses로 제외됨) |
| `checks` | 체크 통과율 상세 |
| `iteration_duration` | VU 1개의 전체 반복 시간 |

## 스테이지 설정 대응표

| Stage | application.yml | 차단 메커니즘 | 거절 위치 |
| --- | --- | --- | --- |
| 1 | version:1, interceptor:false | 없음 (Race Condition 노출) | — |
| 2 | version:2, interceptor:false | @DistributedLock(waitTime=0, leaseTime=4s) | Service AOP 진입 후 |
| 3 | version:3, interceptor:true | IdempotentRequestInterceptor SETNX (TTL 45s) | DispatcherServlet 직후, 컨트롤러 진입 전 |
| 4 | (예정) Virtual Thread + CP 튜닝 | Stage 3과 동일 | 동일 |
