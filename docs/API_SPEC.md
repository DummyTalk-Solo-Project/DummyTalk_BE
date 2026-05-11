# DummyTalk API 명세서

> FE ClaudeCode Agent용. Base URL: `http://localhost:8080` (개발) / 배포 미적용.

---

## 공통

### 공통 응답 구조

모든 API는 아래 JSON Wrapper로 응답합니다.

```json
{
  "isSuccess": true,
  "code": "DUMMY2000",
  "message": "더미 요청에 성공하셨습니다.",
  "result": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `isSuccess` | `boolean` | 성공 여부 (일부 API에서 `success`로 내려올 수 있음) |
| `code` | `string` | 응답 코드 |
| `message` | `string` | 응답 메시지 |
| `result` | `T \| null` | 실제 데이터 (실패 시 null) |

### 인증 Header

인증이 필요한 API는 요청 Header에 Access Token을 포함합니다.

```
Authorization: Bearer: <accessToken>
```

---

## Member API `/api/members`

### 1. 인증 이메일 발송

```
GET /api/members/email-verification
```

**Request**

| 위치 | 파라미터 | 타입 | 필수 | 설명 |
|------|----------|------|------|------|
| Query | `email` | `string` | ✅ | 인증할 이메일 주소 |

**Response**

```json
{
  "isSuccess": true,
  "code": "MEMBER2004",
  "message": "인증 이메일 발송에 성공했습니다.",
  "result": true
}
```

**Error Codes**

| 코드 | 메시지 | 상황 |
|------|--------|------|
| `MEMBER4005` | 이미 이메일을 보냈어요, 다시 한 번 확인해주시겠어요? | 동일 이메일 재발송 |
| `MEMBER4006` | 해당 이메일은 이미 누가 쓰고 있어요. | 이미 가입된 이메일 |
| `SERVER5003` | 서버 에러 (관리자 문의) | 이메일 생성 실패 |
| `SERVER5004` | 서버 에러 (관리자 문의) | 이메일 전송 실패 |

---

### 2. 이메일 코드 검증

```
POST /api/members/verify
```

**Request Body**

```json
{
  "email": "user@example.com",
  "code": "1234"
}
```

**Response**

```json
{
  "isSuccess": true,
  "code": "MEMBER2005",
  "message": "이메일 검증에 성공했습니다",
  "result": true
}
```

**Error Codes**

| 코드 | 메시지 | 상황 |
|------|--------|------|
| `MEMBER4001` | 제가 보낸 이메일이랑 다른 거 같은데, 다시 한 번 확인해보시겠어요? | 인증 코드 불일치 |
| `MEMBER4002` | 이메일이 너무 오래된 거 같은데, 다시 한 번 보내드릴까요? | 인증 코드 만료 |

---

### 3. 회원가입

```
POST /api/members/sign-in
```

**Request Body**

```json
{
  "username": "홍길동",
  "email": "user@example.com",
  "password": "password123"
}
```

**Response**

```json
{
  "isSuccess": true,
  "code": "MEMBER2003",
  "message": "회원가입에 성공했습니다.",
  "result": true
}
```

**Error Codes**

| 코드 | 메시지 | 상황 |
|------|--------|------|
| `MEMBER4004` | 이미 가입한 계정이 있어요 | 중복 가입 시도 |
| `MEMBER4006` | 해당 이메일은 이미 누가 쓰고 있어요. | 이미 존재하는 이메일 |

---

### 4. 로그인

```
POST /api/members/login
```

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response**

```json
{
  "isSuccess": true,
  "code": "MEMBER2001",
  "message": "로그인에 성공했습니다.",
  "result": {
    "isSuccess": true,
    "memberName": "홍길동",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

> 응답 Header `Set-Cookie`에 `refreshToken`(HttpOnly, Secure, 7일) 포함.  
> 응답 Header `Authorization: Bearer: <accessToken>` 포함.

**Error Codes**

| 코드 | HTTP | 메시지 | 상황 |
|------|------|--------|------|
| `MEMBER4007` | 400 | 누군 지 모르겠어요! | 이메일/비밀번호 불일치, 존재하지 않는 회원, 탈퇴 2주 초과 |
| `MEMBER4009` | 403 | 탈퇴한 계정이에요. 2주 이내라면 계정을 되살릴 수 있어요! | **탈퇴 후 2주 이내 재로그인** → FE에서 복구 다이얼로그 표시 후 `/restore` 호출 |

> **탈퇴 계정 복구 플로우:**  
> `MEMBER4009` 수신 → "계정을 복구하시겠습니까?" 다이얼로그 표시 → 확인 클릭 시 `PATCH /api/members/restore` 호출 → 복구 + 자동 로그인

---

### 5. 로그아웃

```
POST /api/members/logout
```

**Request Header** (필수)

```
Authorization: Bearer: <accessToken>
```

**Response**

```json
{
  "isSuccess": true,
  "code": "MEMBER2002",
  "message": "로그아웃에 성공했습니다.",
  "result": true
}
```

**Error Codes**

| 코드 | 메시지 | 상황 |
|------|--------|------|
| `SERVER_4100` | 인증 정보가 없습니다. | Authorization 헤더 누락 |
| `SERVER_4101` | 토큰이 만료되었습니다. | 만료된 Access Token |
| `SERVER_4103` | 블랙리스트에 있는 토큰입니다. | 이미 로그아웃된 토큰 |

---

### 6. 구독 신청

```
POST /api/members/subscribe
```

> 현재 미구현 (항상 `true` 반환). 추후 결제 연동 예정.

**Request Header** (필수)

```
Authorization: Bearer: <accessToken>
```

**Response**

```json
{
  "isSuccess": true,
  "code": "MEMBER2008",
  "message": "구독 요청에 성공했습니다.",
  "result": true
}
```

---

### 7. 마이페이지 조회

```
GET /api/members/my-page
```

**Request Header** (필수)

```
Authorization: Bearer: <accessToken>
```

**Response**

```json
{
  "isSuccess": true,
  "code": "MEMBER2007",
  "message": "사용자 정보 조회에 성공했습니다.",
  "result": [
    {
      "memberName": "홍길동",
      "email": "user@example.com",
      "reqCount": 5,
      "isSubscribe": false,
      "subsExprDate": "2025-12-31T23:59:59",
      "commonStack": 3,
      "rareStack": 2,
      "epicStack": 0
    }
  ]
}
```

> `subsExprDate`: 구독 미가입 시 `null`. ISO 8601 형식.  
> `commonStack` / `rareStack` / `epicStack`: 각 등급의 천장 스택. 10 도달 시 다음 등급 보장.

---

### 8. 회원 탈퇴

```
PATCH /api/members/withdrawal
```

> **Soft Delete 방식.** DB에서 즉시 삭제되지 않으며, `is_deleted=true` + `deleted_at` 기록 후 **2주간 보관** → 이후 스케줄러가 영구 삭제.  
> 탈퇴 즉시 Refresh Token 무효화 (기존 세션 종료).

**Request Header** (필수)

```
Authorization: Bearer: <accessToken>
```

**Response**

```json
{
  "isSuccess": true,
  "code": "MEMBER2006",
  "message": "회원 탈퇴에 성공했습니다.",
  "result": true
}
```

---

### 9. 계정 복구

탈퇴 후 2주 이내에 계정을 복구합니다. 성공 시 자동으로 로그인 상태가 됩니다.

```
PATCH /api/members/restore
```

> 로그인 시 `MEMBER4009` 에러를 받은 경우에만 호출합니다.  
> 복구 성공 시 `/login`과 동일한 응답 구조로 JWT를 발급합니다.

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response**

```json
{
  "isSuccess": true,
  "code": "MEMBER2001",
  "message": "로그인에 성공했습니다.",
  "result": {
    "isSuccess": true,
    "memberName": "홍길동",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

> 응답 Header `Set-Cookie`에 `refreshToken`(HttpOnly, Secure, 7일) 포함.  
> 응답 Header `Authorization: Bearer: <accessToken>` 포함.

**Error Codes**

| 코드 | HTTP | 메시지 | 상황 |
|------|------|--------|------|
| `MEMBER4007` | 400 | 누군 지 모르겠어요! | 이메일/비밀번호 불일치, 탈퇴 상태가 아닌 계정 |
| `MEMBER4010` | 410 | 탈퇴 후 2주가 지나 복구가 불가능해요. | 탈퇴 2주 초과 — 영구 탈퇴 상태 |

---

## Dummy API `/api/dummies`

### 10. 더미(잡지식) 1건 조회

AI가 생성한 잡지식 1건을 가챠 방식으로 반환합니다.

```
GET /api/dummies/dummy
```

**Request Header** (필수)

```
Authorization: Bearer: <accessToken>
```

**Response**

```json
{
  "isSuccess": true,
  "code": "DUMMY2000",
  "message": "더미 요청에 성공하셨습니다.",
  "result": {
    "dummyId": 1,
    "title": "수박의 어원",
    "content": "수박은 원래 서과(西瓜)라 불렸으며...",
    "rarityName": "RARE",
    "isPityTriggered": true,
    "isNextPityTriggered": true,
    "remainingCount": 17
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `dummyId` | `number` | 더미 ID |
| `title` | `string` | 제목 |
| `content` | `string` | 내용 |
| `rarityName` | `string` | 희귀도. `COMMON` \| `RARE` \| `EPIC` \| `SPECIAL` |
| `isPityTriggered` | `boolean` | 이번 뽑기가 천장 발동으로 획득된 경우 `true` |
| `isNextPityTriggered` | `boolean` | 다음 뽑기에서 천장 발동 확정인 경우 `true`. FE에서 "다음 ~~는 무조건~~에요" UI 표시 기준 |
| `remainingCount` | `number` | 오늘 남은 요청 횟수 (최대 20) |

**Error Codes**

| 코드 | 메시지 | 상황 |
|------|--------|------|
| `DUMMY4001` | 모든 무료 요청 횟수를 소모하셨네요, 다음을 기약해주세요 :) | 일일 요청 횟수 초과 |

---

### 11. 내 더미 목록 조회

```
GET /api/dummies/my-dummy?page=0
```

**Request Header** (필수)

```
Authorization: Bearer: <accessToken>
```

**Request Query**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `page` | `number` | `0` | 페이지 번호 (0부터 시작) |

**Response**

```json
{
  "isSuccess": true,
  "code": "DUMMY2000",
  "message": "더미 요청에 성공하셨습니다.",
  "result": [
    {
      "dummyId": 1,
      "title": "수박의 어원",
      "content": "수박은 원래 서과(西瓜)라 불렸으며...",
      "name": "RARE",
      "createdAt": "2025-04-30T12:00:00",
      "rarityId": 2,
      "colorCode": "#FF5733"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `dummyId` | `number` | 더미 ID |
| `title` | `string` | 제목 |
| `content` | `string` | 내용 |
| `name` | `string` | 희귀도. `COMMON` \| `RARE` \| `EPIC` \| `SPECIAL` |
| `createdAt` | `string` | 획득 시각 (ISO 8601) |
| `rarityId` | `number` | 희귀도 ID |
| `colorCode` | `string` | 희귀도 색상 HEX |

---

### 12. 내 더미 키워드 검색

Elasticsearch 기반 한글 형태소 검색.

```
GET /api/dummies/my-dummy/keyword?keyword=수박&page=0
```

**Request Header** (필수)

```
Authorization: Bearer: <accessToken>
```

**Request Query**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `keyword` | `string` | - | 검색 키워드 |
| `page` | `number` | `0` | 페이지 번호 |

**Response** — `11번`과 동일한 구조

---

### 13. 퀴즈 조회

현재 OPEN 상태인 퀴즈 정보를 반환합니다.

```
GET /api/dummies/quiz
```

**Request Header** (필수)

```
Authorization: Bearer: <accessToken>
```

**Response**

```json
{
  "isSuccess": true,
  "code": "DUMMY2002",
  "message": "퀴즈 조회에 성공하셨습니다.",
  "result": {
    "status": "OPEN",
    "userGrade": null,
    "id": 5,
    "title": "다음 중 수박의 원산지는?",
    "answerList": ["동남아시아", "아프리카", "남미", "중앙아시아"]
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `status` | `string` | `NOT_OPEN` \| `OPEN` \| `CLOSE` |
| `userGrade` | `number \| null` | 내 등수 (미구현, 항상 null) |
| `id` | `number` | 퀴즈 ID (BE 실제 필드명. `quizId`로 혼용될 수 있음) |
| `title` | `string` | 문제 |
| `answerList` | `string[]` | 선택지 목록. 인덱스 0 = 첫 번째 선택지 |

**Error Codes**

| 코드 | 메시지 | 상황 |
|------|--------|------|
| `DUMMY4003` | 문제를 풀고 싶은 마음은 알겠지만, 조금만 더 기달려주세요. | OPEN 상태 퀴즈 없음 |

---

### 14. 퀴즈 풀이

```
POST /api/dummies/quiz?id=5&answer=1
```

**Request Header** (필수)

```
Authorization: Bearer: <accessToken>
```

**Request Query**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `number` | 퀴즈 ID |
| `answer` | `number` | 선택한 답 번호 **(1-indexed, 1~4)** |

**Response**

```json
{
  "isSuccess": true,
  "code": "DUMMY2003",
  "message": "퀴즈 풀이에 성공하셨습니다.",
  "result": true
}
```

**Error Codes**

| 코드 | 메시지 | 상황 |
|------|--------|------|
| `DUMMY4003` | 문제를 풀고 싶은 마음은 알겠지만, 조금만 더 기달려주세요. | 퀴즈가 OPEN 아님 |
| `DUMMY4004` | 알 수 없는 퀴즈를 풀고 계신 것 같아요. | 존재하지 않는 id |
| `DUMMY4005` | 좋은 발상이었는데, 아쉽게도 정답이 아니에요. | 오답 또는 범위 밖 answer |
| `DUMMY4006` | 한 번 푸셨던 문제는 다시 풀 수 없어요. 다른 사용자에게 배려해주세요 :) | 중복 제출 |
| `DUMMY4007` | 퀴즈는 풀었지만 이제 티켓을 받을 수는 없네요 | 티켓(선착순 한도) 소진 |
| `QUIZ4007` | 아쉽게도 퀴즈가 닫혔어요.... | 풀이 도중 스케줄러에 의해 퀴즈 CLOSE됨 |

---

### 15. 퀴즈 오픈 (Admin Only)

퀴즈를 생성하고 오픈 스케줄을 등록합니다.

```
POST /api/dummies/open-quiz?open-time=2025-05-01T18:00:00
```

**Request Header** (필수, Admin 계정)

```
Authorization: Bearer: <adminAccessToken>
```

**Request Query**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `open-time` | `string` | 퀴즈 오픈 시각. ISO 8601 (`yyyy-MM-ddTHH:mm:ss`) |

**Response**

```json
{
  "isSuccess": true,
  "code": "DUMMY2001",
  "message": "(Only Admin) 퀴즈 오픈에 성공하셨습니다.",
  "result": {
    "id": 1,
    "title": "다음 중 수박의 원산지는?",
    "answerList": ["동남아시아", "아프리카", "남미", "중앙아시아"],
    "answer": 2,
    "description": "수박은 아프리카 사막 지대가 원산지입니다.",
    "ticket": 5,
    "status": "NOT_OPEN",
    "startTime": "2025-05-01T18:00:00",
    "endTime": "2025-05-01T18:05:00"
  }
}
```

> 오픈 후 5분 뒤 자동으로 `CLOSE` 처리됩니다.

**Error Codes**

| 코드 | 메시지 | 상황 |
|------|--------|------|
| `SERVER_4300` | 접근 권한이 없습니다. | Admin 권한 없음 |

---

### 16. 퀴즈 스케줄러 상태 확인 (Admin Only)

```
GET /api/dummies/check-quiz
```

**Request Header** (필수, Admin 계정)

```
Authorization: Bearer: <adminAccessToken>
```

**Response**

```json
{
  "isSuccess": true,
  "code": "DUMMY2004",
  "message": "퀴즈 체킹에 성공하셨습니다.",
  "result": {
    "activeCount": 1,
    "poolSize": 3
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `activeCount` | `number` | 현재 실행 중인 스케줄러 스레드 수 |
| `poolSize` | `number` | 스케줄러 스레드 풀 크기 |

---

## 공통 에러 코드

### 인증/보안

| 코드 | HTTP | 메시지 | 상황 |
|------|------|--------|------|
| `SERVER_4100` | 401 | 인증 정보가 없습니다. | Authorization 헤더 없음 |
| `SERVER_4101` | 401 | 토큰이 만료되었습니다. | Access Token 만료 → 토큰 재발급 필요 |
| `SERVER_4102` | 401 | 유효하지 않은 토큰입니다. | 잘못된 형식의 토큰 |
| `SERVER_4103` | 401 | 블랙리스트에 있는 토큰입니다. | 로그아웃 처리된 토큰 |
| `SERVER_4104` | 401 | 리프레쉬 토큰을 찾을 수 없습니다. | Refresh Token 없음 |
| `SERVER_4300` | 403 | 접근 권한이 없습니다. | MEMBER 권한으로 Admin 전용 API 호출 |

### 회원 탈퇴/복구

| 코드 | HTTP | 메시지 | 상황 |
|------|------|--------|------|
| `MEMBER4008` | 401 | 이미 탈퇴한 계정입니다. | 일반 탈퇴 에러 (미사용, 하위 호환용) |
| `MEMBER4009` | 403 | 탈퇴한 계정이에요. 2주 이내라면 계정을 되살릴 수 있어요! | 탈퇴 후 2주 이내 로그인 → 복구 다이얼로그 트리거 |
| `MEMBER4010` | 410 | 탈퇴 후 2주가 지나 복구가 불가능해요. | 탈퇴 후 2주 초과 복구 시도 |

### 서버

| 코드 | HTTP | 상황 |
|------|------|------|
| `SERVER5000` | 500 | 일반 서버 에러 |
| `SERVER5001` | 500 | 파싱 에러 |
| `SERVER5002` | 500 | AI 파싱 에러 |

---

## 타입 참조

### RarityType (희귀도)

| 값 | 설명 | 확률 (예정) |
|----|------|------------|
| `COMMON` | 일반 | 55% |
| `RARE` | 희귀 | 30% |
| `EPIC` | 에픽 | 12% |
| `SPECIAL` | 스페셜 | 3% |

### QuizStatus (퀴즈 상태)

| 값 | 설명 |
|----|------|
| `NOT_OPEN` | 오픈 대기 |
| `OPEN` | 진행 중 |
| `CLOSE` | 종료 |
