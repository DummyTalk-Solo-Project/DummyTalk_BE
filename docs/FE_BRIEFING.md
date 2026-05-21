# DummyTalk FE 연동 브리핑

> FE Claude Code Agent용. BE 연동 전 반드시 숙지할 것.
> API 상세 명세는 `docs/API_SPEC.md` 참고.

---

## 1. 엔드포인트

| 환경 | Base URL |
|------|----------|
| 로컬 개발 | `http://localhost:8080` |
| 배포 | `https://ddotg.dev` |

---

## 2. 인증 구조

DummyTalk은 **AT(Access Token) + RT(Refresh Token)** 이중 토큰 구조입니다.

| 토큰 | 전달 방식 | 만료 |
|------|----------|------|
| AT | `Authorization` 요청 헤더 | 1시간 |
| RT | `HttpOnly` 쿠키 (자동 전송) | 7일 |

### ⚠️ Bearer 형식 — 반드시 확인

```
✅ Authorization: Bearer <token>       ← 표준 (RFC 6750)
❌ Authorization: Bearer: <token>      ← 비표준, BE가 파싱 불가
```

콜론(`:`) 없이 공백 하나만 사용합니다.

---

## 3. 필수 전역 설정

```js
// axios 사용 시 — 프로젝트 최상단에서 1회 설정
axios.defaults.withCredentials = true;
axios.defaults.baseURL = 'https://ddotg.dev'; // 배포 환경
```

`withCredentials: true` 없으면 RT 쿠키가 전송되지 않아 **AT 갱신이 불가**합니다.

---

## 4. AT 자동 갱신 인터셉터 (필수 구현)

별도 갱신 엔드포인트 없음. AT 만료 후 요청 시 BE 필터가 자동 갱신합니다.
**모든 응답**의 `Authorization` 헤더를 체크해 새 AT가 오면 갱신하는 인터셉터를 구현해야 합니다.

```js
axios.interceptors.response.use((response) => {
  const newToken = response.headers['authorization'];
  if (newToken?.startsWith('Bearer ')) {
    const token = newToken.slice(7);
    // 저장소(메모리 or localStorage)에 AT 갱신
    saveAccessToken(token);
  }
  return response;
});
```

> `Authorization` 헤더는 BE에서 `exposeHeaders`로 명시적으로 노출하고 있어 JS에서 읽기 가능합니다.

---

## 5. RT 쿠키

- `HttpOnly` → JS에서 직접 읽기/쓰기 불가
- `SameSite=None; Secure` → 크로스 도메인(Vercel ↔ ddotg.dev) 전송 가능
- `withCredentials: true` 설정 시 브라우저가 자동 첨부/수신

FE가 쿠키를 직접 조작할 필요 없습니다.

---

## 6. CORS 허용 Origin

아래 목록 외 Origin은 BE에서 차단됩니다.

| Origin | 용도 |
|--------|------|
| `https://dummytalk.vercel.app` | Vercel 배포 ← 실제 배포 URL로 확인 필요 |
| `http://localhost:3000` | 로컬 개발 |
| `http://localhost:5173` | 로컬 개발 (Vite) |

> Vercel 배포 도메인이 `dummytalk.vercel.app`과 다르면 BE SecurityConfig 수정 필요. BE 담당자에게 알릴 것.

---

## 7. 로그인 플로우

```
POST /api/members/login
Body: { email, password }

성공 응답:
  Header → Authorization: Bearer <accessToken>
  Cookie → refreshToken=...; HttpOnly; Secure; SameSite=None
  Body   → { isSuccess, memberName, accessToken, needPasswordChange }
```

- `accessToken`은 Body와 Header 두 곳 모두 옴. 둘 중 하나만 사용하면 됨.
- `needPasswordChange: true`이면 비밀번호 변경 페이지로 유도.

### 탈퇴 계정 복구 플로우

로그인 시 `MEMBER4009` 에러 수신 → "계정을 복구하시겠습니까?" 다이얼로그 표시 → 확인 클릭 시 `PATCH /api/members/restore` 호출 (body는 로그인과 동일).

---

## 8. 공통 에러 응답 구조

```json
{
  "isSuccess": false,
  "code": "SERVER_4101",
  "message": "토큰이 만료되었습니다.",
  "result": null
}
```

| 코드 | 상황 | FE 처리 |
|------|------|---------|
| `SERVER_4100` | AT 없음 | 로그인 페이지 이동 |
| `SERVER_4101` | AT 만료 | 인터셉터가 자동 갱신 (별도 처리 불필요) |
| `SERVER_4103` | 블랙리스트 토큰 | 강제 로그아웃 |
| `SERVER_4104` | RT 없음/만료 | 강제 로그아웃 후 로그인 페이지 이동 |
| `SERVER_4300` | 권한 없음 (비Admin) | 403 페이지 또는 알림 |

---

## 9. 구현 전 체크리스트

- [ ] `axios.defaults.withCredentials = true` 전역 설정
- [ ] `Authorization: Bearer <token>` (콜론 없음) 형식으로 헤더 전송
- [ ] 응답 인터셉터에서 `Authorization` 헤더 체크 → AT 갱신 처리
- [ ] Vercel 배포 도메인이 `dummytalk.vercel.app`인지 확인 (다르면 BE에 알릴 것)
- [ ] 로컬 개발 시 BE `localhost:8080` 또는 `ddotg.dev` 중 선택 후 baseURL 설정
