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

### JWT 클레임 구조

| 클레임 | 키 | 값 예시 | 설명 |
|--------|-----|---------|------|
| Subject | `sub` | `user@example.com` | 이메일 (사용자 식별자) |
| 권한 (Spring Security) | `auth` | `ADMIN` | 서버 내부 인증용 |
| 권한 (FE) | `role` | `ADMIN` | FE 권한 체크용. `ADMIN` 또는 `MEMBER` |
| 만료 | `exp` | - | 표준 만료 시각 (Unix timestamp) |

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

## Notice API `/api/notices`

> 인증 불필요. 공개된 공지사항만 반환.

### 15. 공지사항 목록 조회

```
GET /api/notices?page=0
```

**Request Query**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `page` | `number` | `0` | 페이지 번호 (0부터, 페이지당 20개) |

**Response**

```json
{
  "isSuccess": true,
  "code": "NOTICE2001",
  "message": "공지사항 조회에 성공했습니다.",
  "result": [
    {
      "id": 1,
      "title": "서비스 점검 안내",
      "isPinned": true,
      "isPublished": true,
      "authorName": "관리자",
      "createdAt": "2025-05-01T12:00:00"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `number` | 공지사항 ID |
| `title` | `string` | 제목 |
| `isPinned` | `boolean` | 상단 고정 여부 — `true`인 항목이 목록 최상단 |
| `isPublished` | `boolean` | 공개 여부 (목록에서는 항상 `true`) |
| `authorName` | `string \| null` | 작성자 이름 |
| `createdAt` | `string` | 작성일시 (ISO 8601) |

---

### 16. 공지사항 상세 조회

```
GET /api/notices/{id}
```

**Response**

```json
{
  "isSuccess": true,
  "code": "NOTICE2001",
  "message": "공지사항 조회에 성공했습니다.",
  "result": {
    "id": 1,
    "title": "서비스 점검 안내",
    "content": "5월 10일 02:00~04:00 서비스 점검이 예정되어 있습니다.",
    "isPinned": true,
    "isPublished": true,
    "authorName": "관리자",
    "createdAt": "2025-05-01T12:00:00",
    "updatedAt": "2025-05-02T09:00:00"
  }
}
```

**Error Codes**

| 코드 | HTTP | 메시지 | 상황 |
|------|------|--------|------|
| `NOTICE4001` | 404 | 공지사항을 찾을 수 없습니다. | 존재하지 않거나 삭제된 ID |
| `NOTICE4002` | 404 | 공개되지 않은 공지사항입니다. | `isPublished=false`인 공지사항 접근 |

---

## Admin API `/api/admin`

> **모든 Admin API는 Admin 계정 필수.** 일반 MEMBER 권한으로 호출 시 `SERVER_4300` 반환.

### 대시보드

#### 17. 정산 단건 조회

```
GET /api/admin/dashboard/daily?date=2025-05-10
```

**Request Header** (필수, Admin)

```
Authorization: Bearer: <adminAccessToken>
```

**Request Query**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `date` | `string` | 조회 날짜. ISO 8601 (`yyyy-MM-dd`). 오늘 날짜는 불가 (00:30에 전날치 저장) |

**Response**

```json
{
  "isSuccess": true,
  "code": "ADMIN2001",
  "message": "정산 데이터 조회에 성공했습니다.",
  "result": {
    "settlementDate": "2025-05-10",
    "totalDummyViews": 1250,
    "newMemberCount": 15,
    "commonCount": 800,
    "rareCount": 300,
    "epicCount": 120,
    "specialCount": 30,
    "activeMemberCount": 430,
    "activeSubscriberCount": 55
  }
}
```

**Error Codes**

| 코드 | HTTP | 메시지 | 상황 |
|------|------|--------|------|
| `ADMIN4001` | 404 | 해당 날짜의 정산 데이터가 없습니다. | 해당 날짜 데이터 미존재 |

---

#### 18. 기간별 정산 목록 조회

```
GET /api/admin/dashboard/range?from=2025-05-01&to=2025-05-10
```

**Request Query**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `from` | `string` | 시작 날짜 (`yyyy-MM-dd`) |
| `to` | `string` | 종료 날짜 (`yyyy-MM-dd`) |

**Response** — `result`가 `17번` 단건 DTO의 배열. 날짜 오름차순 정렬.

```json
{
  "isSuccess": true,
  "code": "ADMIN2001",
  "message": "정산 데이터 조회에 성공했습니다.",
  "result": [
    { "settlementDate": "2025-05-01", "totalDummyViews": 980, ... },
    { "settlementDate": "2025-05-02", "totalDummyViews": 1050, ... }
  ]
}
```

---

#### 19. 최근 N일 정산 목록 조회

```
GET /api/admin/dashboard/latest?days=7
```

**Request Query**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `days` | `number` | `7` | 최근 며칠치 조회. 어제 기준 N일 전 ~ 어제 |

**Response** — `result`가 `17번` 단건 DTO의 배열. 날짜 오름차순 정렬.

---

### 공지사항 관리

#### 20. 공지사항 전체 목록 조회 (비공개 포함)

```
GET /api/admin/notices?page=0
```

**Request Query**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `page` | `number` | `0` | 페이지 번호 (페이지당 20개) |

**Response** — `15번`과 동일한 구조. `isPublished=false`(임시저장) 포함.

---

#### 21. 공지사항 상세 조회 (비공개 포함)

```
GET /api/admin/notices/{id}
```

**Response** — `16번`과 동일한 구조. 비공개 공지사항도 조회 가능.

---

#### 22. 공지사항 작성

```
POST /api/admin/notices
```

**Request Body**

```json
{
  "title": "서비스 점검 안내",
  "content": "5월 10일 02:00~04:00 서비스 점검이 예정되어 있습니다.",
  "isPinned": false
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | `string` | ✅ | 제목 |
| `content` | `string` | ✅ | 본문 |
| `isPinned` | `boolean` | - | 상단 고정 여부. 미전송 시 `false` |

**Response**

```json
{
  "isSuccess": true,
  "code": "NOTICE2002",
  "message": "공지사항 작성에 성공했습니다.",
  "result": {
    "id": 3,
    "title": "서비스 점검 안내",
    "content": "5월 10일 02:00~04:00 서비스 점검이 예정되어 있습니다.",
    "isPinned": false,
    "isPublished": false,
    "authorName": "관리자",
    "createdAt": "2025-05-19T10:00:00",
    "updatedAt": "2025-05-19T10:00:00"
  }
}
```

> 작성 직후 `isPublished=false` (임시저장 상태). 공개하려면 `22번` 토글 호출.

---

#### 23. 공지사항 수정

```
PATCH /api/admin/notices/{id}
```

> 부분 수정 — 전송하지 않은 필드(`null`)는 변경되지 않습니다.

**Request Body**

```json
{
  "title": "수정된 제목",
  "content": null,
  "isPinned": true
}
```

**Response** — `22번`과 동일한 구조 (수정된 상태 반환).

```json
{
  "isSuccess": true,
  "code": "NOTICE2003",
  "message": "공지사항 수정에 성공했습니다.",
  "result": { ... }
}
```

**Error Codes**

| 코드 | HTTP | 메시지 | 상황 |
|------|------|--------|------|
| `NOTICE4001` | 404 | 공지사항을 찾을 수 없습니다. | 존재하지 않거나 이미 삭제된 ID |

---

#### 24. 공지사항 삭제 (Soft Delete)

```
DELETE /api/admin/notices/{id}
```

**Response**

```json
{
  "isSuccess": true,
  "code": "NOTICE2004",
  "message": "공지사항 삭제에 성공했습니다.",
  "result": true
}
```

**Error Codes**

| 코드 | HTTP | 메시지 | 상황 |
|------|------|--------|------|
| `NOTICE4001` | 404 | 공지사항을 찾을 수 없습니다. | 존재하지 않거나 이미 삭제된 ID |

---

#### 25. 공지사항 공개/비공개 토글

```
PATCH /api/admin/notices/{id}/publish
```

**Response** — `result`는 변경 후 현재 `isPublished` 값.

```json
{
  "isSuccess": true,
  "code": "NOTICE2005",
  "message": "공지사항 공개 상태가 변경되었습니다.",
  "result": true
}
```

**Error Codes**

| 코드 | HTTP | 메시지 | 상황 |
|------|------|--------|------|
| `NOTICE4001` | 404 | 공지사항을 찾을 수 없습니다. | 존재하지 않거나 이미 삭제된 ID |

---

### 퀴즈 / 회원 관리

#### 26. 퀴즈 오픈 (Admin Only)

퀴즈를 생성하고 오픈 스케줄을 등록합니다.

```
POST /api/admin/quiz/open?open-time=2025-05-01T18:00:00
```

**Request Query**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `open-time` | `string` | 퀴즈 오픈 시각. ISO 8601 (`yyyy-MM-ddTHH:mm:ss`) |

**Response**

```json
{
  "isSuccess": true,
  "code": "ADMIN2002",
  "message": "(Admin) 퀴즈 오픈에 성공했습니다.",
  "result": {
    "id": 1,
    "title": "다음 중 수박의 원산지는?",
    "answerList": ["동남아시아", "아프리카", "남미", "중앙아시아"],
    "answer": 2,
    "description": "수박은 아프리카 사막 지대가 원산지입니다.",
    "ticket": 10,
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
| `QUIZ4008` | 퀴즈 오픈 시간은 현재 시간 이후여야 해요. | 과거 시간으로 오픈 시도 |

---

#### 27. 퀴즈 스케줄러 상태 확인 (Admin Only)

```
GET /api/admin/check-quiz
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

#### 28. 구독 승인 (Admin Only)

```
PATCH /api/admin/members/subscribe?email=user@example.com
```

**Request Query**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `email` | `string` | 승인할 회원 이메일 |

**Response**

```json
{
  "isSuccess": true,
  "code": "ADMIN2003",
  "message": "(Admin) 구독 승인에 성공했습니다.",
  "result": true
}
```

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

### 공지사항

| 코드 | HTTP | 메시지 | 상황 |
|------|------|--------|------|
| `NOTICE4001` | 404 | 공지사항을 찾을 수 없습니다. | 존재하지 않거나 삭제된 ID |
| `NOTICE4002` | 404 | 공개되지 않은 공지사항입니다. | 비공개 공지사항 일반 사용자 접근 |

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

---

## MVP 현황 (2026-05-19 기준)

| 도메인 | 기능 | 상태 |
|--------|------|------|
| Member | 이메일 인증/회원가입/로그인/로그아웃/탈퇴/복구/마이페이지 | ✅ 완성 |
| Member | 구독 신청 (`/subscribe`) | ⚠️ 항상 true 반환 (결제 미연동) |
| Dummy | 가챠/목록/검색/퀴즈 조회·풀이 | ✅ 완성 |
| Notice | 공개 목록/상세 조회 | ✅ 완성 |
| Admin | 공지사항 CRUD + 공개 토글 | ✅ 완성 |
| Admin | 정산 단건/기간/최근N일 조회 | ✅ 완성 |
| Admin | 퀴즈 오픈/스케줄러 확인/구독 승인 | ✅ 완성 |
| Member | 연속 출석 체크 | ❌ 미구현 |
| Global | 분산락 (Redisson) | ❌ 미구현 |
| Global | HTTPS/도메인 | ❌ 미적용 |