import http from 'k6/http';
import { check } from 'k6';

/*
 * DummyTalk open model 부하 테스트 공용 모듈 (테스트 본체 아님)
 *
 * 역할 4가지 — dummy-arrival-test.js 가 이 함수들을 조합해서 회차를 돌린다.
 *   1. loginPool()           : 테스트 유저 풀 병렬 로그인 → { userNum: accessToken }
 *   2. adminLogin/reset/state: /api/admin/load-test/* 호출 (회차 사이 리셋, Lost Update 판정용 스냅샷)
 *   3. scrapeBuckets/quantile: /actuator/prometheus 히스토그램 델타로 서버측 p50/p95/p99 (RTT 제외)
 *   4. summaryFiles()        : CLAUDE_INIT 파일명 규칙으로 dev_notes/DummyTalk/results 에 .txt + .json 저장
 *
 * Lumo_Backend/k6/lib/lumo.js 에서 검증된 패턴을 DummyTalk 엔드포인트에 맞게 이식했다.
 */

export const BASE = __ENV.BASE_URL || 'http://localhost:8080';
export const TARGET_URI = '/api/dummies/dummy'; // 서버측 히스토그램은 이 uri 태그로 걸러 본다
export const TEST_PASSWORD = 'Test1234!';       // TestMemberDataLoader 가 만든 test%@test.com 공통 비밀번호

// ─── 1. 로그인 ───────────────────────────────────────────────────────────────

function loginRequest(email, password) {
    return {
        method: 'POST',
        url: `${BASE}/api/members/login`,
        body: JSON.stringify({ email, password }),
        params: { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } },
    };
}

/**
 * test1~N@test.com 을 batchSize 개씩 병렬 로그인한다.
 *
 * 순차 로그인은 BCrypt(서버측 ~100ms) × N 이라 1,500명이면 수 분이 걸린다. http.batch 는 한 VU 안에서
 * 요청을 동시에 보내므로 setup 시간을 배치 수만큼으로 줄인다. setup 구간이라 측정 지표에는 섞이지 않는다.
 *
 * @returns { tokens: {userNum: token}, failed: number }
 */
export function loginPool(n, batchSize) {
    const size = batchSize || 50;
    const tokens = {};
    let failed = 0;

    for (let start = 1; start <= n; start += size) {
        const end = Math.min(n, start + size - 1);
        const reqs = [];
        for (let i = start; i <= end; i++) {
            reqs.push(loginRequest(`test${i}@test.com`, TEST_PASSWORD));
        }
        const responses = http.batch(reqs);
        for (let j = 0; j < responses.length; j++) {
            const userNum = start + j;
            const res = responses[j];
            let token = null;
            try { token = res.status === 200 ? res.json('result.accessToken') : null; } catch (e) { token = null; }
            if (token) {
                tokens[String(userNum)] = token;
            } else {
                failed++;
                if (failed <= 5) console.error(`[loginPool] test${userNum}@test.com 로그인 실패 status=${res.status}`);
            }
        }
        if ((end % 500) === 0 || end === n) console.log(`[loginPool] ${end}/${n} 로그인 진행 (실패 ${failed})`);
    }
    return { tokens, failed };
}

// ─── 2. Admin 부하테스트 API ────────────────────────────────────────────────

/** 관리자 로그인. 자격증명은 -e ADMIN_EMAIL / ADMIN_PASSWORD 로만 받는다 (스크립트에 남기지 않음). */
export function adminLogin() {
    const email = __ENV.ADMIN_EMAIL;
    const password = __ENV.ADMIN_PASSWORD;
    if (!email || !password) {
        console.warn('[adminLogin] ADMIN_EMAIL/ADMIN_PASSWORD 미지정 → 리셋·Lost Update 판정을 건너뜁니다');
        return null;
    }
    const res = http.post(`${BASE}/api/members/login`, JSON.stringify({ email, password }),
        { headers: { 'Content-Type': 'application/json' }, tags: { name: 'admin-login' } });
    const token = res.status === 200 ? res.json('result.accessToken') : null;
    if (!token) console.error(`[adminLogin] 관리자 로그인 실패 status=${res.status}`);
    return token;
}

function adminParams(token, name) {
    return {
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        tags: { name },
    };
}

/** 테스트 유저 reqCount=0 + 구독자(40회) 전환. @returns 갱신 건수 (실패 시 null) */
export function loadTestReset(adminToken) {
    if (!adminToken) return null;
    const res = http.post(`${BASE}/api/admin/load-test/reset`, null, adminParams(adminToken, 'load-test-reset'));
    check(res, { 'load-test/reset 200': (r) => r.status === 200 });
    if (res.status !== 200) { console.error(`[loadTestReset] status=${res.status} body=${res.body}`); return null; }
    return res.json('result');
}

/**
 * 테스트 유저 수·reqCount 합·서버 동시성 설정.
 * reqCountSum 은 setup/teardown 두 번 찍어 델타 = "DB 가 실제 반영한 뽑기 수" 를 얻는다.
 */
export function loadTestState(adminToken) {
    if (!adminToken) return null;
    const res = http.get(`${BASE}/api/admin/load-test/state`, adminParams(adminToken, 'load-test-state'));
    if (res.status !== 200) { console.error(`[loadTestState] status=${res.status} body=${res.body}`); return null; }
    return res.json('result');
}

/** 서버 설정 → 파일명 특이사항 태그 (카멜케이스). 예) v3InterceptorVtCp10 */
export function tagFromState(state) {
    if (!state) return 'unknownConfig';
    return `v${state.getDummyVersion}`
        + (state.interceptorEnabled ? 'Interceptor' : 'NoInterceptor')
        + (state.virtualThreads ? 'Vt' : 'Pt')
        + `Cp${state.hikariPoolSize}`;
}

// ─── 3. 서버측 히스토그램 (RTT 제외) ───────────────────────────────────────

/**
 * http_server_requests_seconds_bucket 을 긁어 { le: 누적건수 } 로 돌려준다.
 *
 * 왜 서버측 값을 따로 보는가 — k6 의 http_req_duration 은 부하 생성기 ↔ 서버 RTT(+Cloudflare 경유)가 섞인다.
 * 이 히스토그램은 Tomcat 이 요청을 받아 응답을 쓸 때까지만 재므로 개선 전후 비교는 이 값으로 한다.
 * 같은 le 라도 status/outcome 조합마다 라인이 갈리므로 합산해야 전체 분포가 된다.
 * le 라벨 위치는 Micrometer/Prometheus 클라이언트 버전마다 달라서 위치에 의존하지 않고 찾는다.
 */
export function scrapeBuckets(uri) {
    const res = http.get(`${BASE}/actuator/prometheus`, { tags: { name: 'scrape' } });
    if (res.status !== 200) { console.warn(`[scrapeBuckets] status=${res.status}`); return null; }

    const buckets = {};
    const lines = res.body.split('\n');
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        if (line.indexOf('http_server_requests_seconds_bucket{') !== 0) continue;
        if (uri && line.indexOf(`uri="${uri}"`) < 0) continue;
        const leMatch = /le="([^"]+)"/.exec(line);
        const valMatch = /\}\s+([0-9.eE+-]+)(\s+[0-9.eE+-]+)?\s*$/.exec(line);
        if (!leMatch || !valMatch) continue;
        const le = leMatch[1] === '+Inf' ? 'Infinity' : String(parseFloat(leMatch[1]));
        const count = parseFloat(valMatch[1]);
        if (isNaN(count)) continue;
        buckets[le] = (buckets[le] || 0) + count;
    }
    return Object.keys(buckets).length ? buckets : null;
}

/**
 * 두 스냅샷의 차이로 회차 분위수 (Prometheus histogram_quantile 과 같은 선형 보간). 단위: 초. 표본 없으면 null.
 * 누적 히스토그램이라 델타도 단조 증가를 유지하므로 뺀 뒤 그대로 계산할 수 있다.
 */
export function quantileFromBuckets(base, now, q) {
    if (!now) return null;
    const les = Object.keys(now)
        .map((k) => (k === 'Infinity' ? Infinity : parseFloat(k)))
        .sort((a, b) => a - b);
    if (!les.length) return null;

    const cum = les.map((le) => {
        const key = le === Infinity ? 'Infinity' : String(le);
        return (now[key] || 0) - (base && base[key] !== undefined ? base[key] : 0);
    });
    const total = cum[cum.length - 1];
    if (!total || total <= 0) return null;

    const target = total * q;
    for (let i = 0; i < les.length; i++) {
        if (cum[i] < target) continue;
        if (les[i] === Infinity) return i > 0 ? les[i - 1] : null; // 상한 밖 — 버킷 최대값을 올려야 한다는 신호
        const lowerLe = i === 0 ? 0 : les[i - 1];
        const lowerCum = i === 0 ? 0 : cum[i - 1];
        const span = cum[i] - lowerCum;
        if (span <= 0) return les[i];
        return lowerLe + (les[i] - lowerLe) * ((target - lowerCum) / span);
    }
    return les[les.length - 1];
}

/** 회차 구간에 서버가 처리한 총 건수 (+Inf 버킷 델타) */
export function bucketTotal(base, now) {
    if (!now) return 0;
    const b = base && base.Infinity !== undefined ? base.Infinity : 0;
    return (now.Infinity || 0) - b;
}

// ─── 4. 결과 파일 저장 ─────────────────────────────────────────────────────

function pad2(n) { return n < 10 ? '0' + n : String(n); }

/** `20260923_1400` — CLAUDE_INIT 파일명 규칙의 앞부분 */
export function stamp(d) {
    return d.getFullYear() + pad2(d.getMonth() + 1) + pad2(d.getDate())
        + '_' + pad2(d.getHours()) + pad2(d.getMinutes());
}

export function isoLocal(d) {
    return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate())
        + ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds());
}

/** '3m' / '90s' / '2m30s' / '1h' → 초 */
export function parseDurationSec(s) {
    const re = /(\d+(?:\.\d+)?)\s*(h|ms|m|s)/g;
    let total = 0, m, matched = false;
    while ((m = re.exec(String(s))) !== null) {
        matched = true;
        const v = parseFloat(m[1]);
        total += m[2] === 'h' ? v * 3600 : m[2] === 'm' ? v * 60 : m[2] === 's' ? v : v / 1000;
    }
    return matched ? total : parseFloat(s) || 0;
}

/** k6 요약 객체를 사람이 읽는 텍스트로. 외부 jslib 에 의존하지 않는다 (오프라인 안전). */
export function renderSummary(data) {
    const lines = [];
    const ms = (v) => (Math.round(v * 100) / 100) + 'ms';

    const checks = data.metrics && data.metrics.checks;
    if (checks && checks.values) {
        const p = checks.values.passes || 0, f = checks.values.fails || 0;
        lines.push(`CHECKS  성공 ${p} / 실패 ${f}  (${((p / Math.max(1, p + f)) * 100).toFixed(2)}%)`, '');
    }

    lines.push('METRICS');
    Object.keys(data.metrics).sort().forEach((n) => {
        const m = data.metrics[n], v = m.values || {};
        let body;
        if (m.type === 'counter') body = `count=${v.count}  rate=${(v.rate || 0).toFixed(4)}/s`;
        else if (m.type === 'rate') body = `${(v.rate * 100).toFixed(2)}%  (passes=${v.passes} fails=${v.fails})`;
        else if (m.type === 'gauge') body = `value=${v.value}  min=${v.min}  max=${v.max}`;
        else {
            const u = m.contains === 'time' ? ms : (x) => String(Math.round(x * 100) / 100);
            body = `avg=${u(v.avg)}  min=${u(v.min)}  med=${u(v.med)}  max=${u(v.max)}`
                + `  p90=${u(v['p(90)'])}  p95=${u(v['p(95)'])}  p99=${u(v['p(99)'] !== undefined ? v['p(99)'] : v.max)}`;
        }
        let line = '  ' + n; while (line.length < 34) line += ' ';
        lines.push(line + ': ' + body);
        if (m.thresholds) {
            Object.keys(m.thresholds).forEach((t) => lines.push(`      threshold ${t} → ${m.thresholds[t].ok ? 'PASS' : 'FAIL'}`));
        }
    });
    return lines.join('\n');
}

/**
 * handleSummary 가 돌려줄 파일 맵.
 *
 * 저장 경로 우선순위: -e RESULT_DIR > 시스템 env DUMMYTALK_RESULT_DIR > 상대경로 기본값
 * (레포 루트 Github/Project/DummyTalk_BE 에서 실행할 때 ../../dev_notes/DummyTalk/results).
 * ⚠️ k6 는 디렉터리를 만들어 주지 않는다 — 경로가 없으면 회차 로그가 통째로 사라지므로 stdout 에 경로를 찍는다.
 * ⚠️ 'stdout' 키를 돌려주면 k6 기본 요약이 대체되므로 직접 렌더링한 요약을 같이 넣는다.
 *
 * @param meta { rate, duration, tag, scenario, processingReport, params, extra }  extra = JSON 에 함께 남길 판정값
 */
export function summaryFiles(data, meta) {
    const endedAt = new Date();
    const durMs = (data.state && data.state.testRunDurationMs) || 0;
    const startedAt = new Date(endedAt.getTime() - durMs);

    const name = `${stamp(startedAt)}_${meta.rate}rps_${meta.duration}_${meta.tag}`;
    const dir = __ENV.RESULT_DIR || __ENV.DUMMYTALK_RESULT_DIR || '../../dev_notes/DummyTalk/results';
    const base = `${dir}/${name}`;

    const head = [
        '='.repeat(78),
        ` 회차: ${name}`,
        ` 시나리오: ${meta.scenario || '-'}`,
        ` 대상: ${BASE}   URI: ${TARGET_URI}`,
        '',
        ` 시작: ${isoLocal(startedAt)}`,
        ` 종료: ${isoLocal(endedAt)}`,
        ` 소요: ${(durMs / 1000).toFixed(1)}s  (setup 로그인 포함 — 부하 구간은 ${meta.duration})`,
        '',
        ' ※ Grafana 시간 범위를 위 시작~종료로 맞출 것. 부하 구간 밖이 섞이면 평균이 희석된다.',
        ` ※ 대시보드 캡쳐는 grafana_dashboards_capture/${name} - 1~4.jpg 로 저장한다.`,
        '='.repeat(78),
        '',
    ].join('\n');

    const tail = [
        '', '', '='.repeat(78), ' 분석', '='.repeat(78),
        '(2번 에이전트가 적는다 — 어느 지표가 먼저 포화했고, 이전 회차와 무엇이 달라졌는가)', '',
    ].join('\n');

    const report = meta.processingReport || '';
    const text = head + report + '\n\n' + renderSummary(data) + tail;

    const json = {
        run: name,
        scenario: meta.scenario,
        base_url: BASE,
        started_at: isoLocal(startedAt),
        ended_at: isoLocal(endedAt),
        params: meta.params || {},
        verdict: meta.extra || {},
        metrics: data.metrics,
    };

    const out = {};
    out[`${base}.txt`] = text;
    out[`${base}.json`] = JSON.stringify(json, null, 2);
    out['stdout'] = report + '\n\n' + renderSummary(data) + '\n\n'
        + '── 저장 ' + '─'.repeat(60) + '\n'
        + `  시작 ${isoLocal(startedAt)}  →  종료 ${isoLocal(endedAt)}   (${(durMs / 1000).toFixed(1)}s)\n`
        + `  ${base}.txt\n  ${base}.json\n`
        + `  캡쳐 파일명: ${name} - 1.jpg ~ - 4.jpg\n`
        + '─'.repeat(68) + '\n';
    return out;
}
