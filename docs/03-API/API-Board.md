---
title: API Board
tags: [api, board]
updated: 2026-08-01
---

# API: 게시판 (`/api/board`)

`board/BoardController.java`

조회는 공개, 나머지는 **ADMIN 전용**.

---

## GET `/api/board/all` — 게시판 목록

권한: 🌐 공개

### 응답 `200 OK`

```json
[
  { "id": 1, "name": "자유게시판", "description": "자유롭게 이야기하세요", "createdAt": "2026-07-21T10:00:00" },
  { "id": 2, "name": "공지사항",   "description": null,                    "createdAt": "2026-07-21T10:05:00" }
]
```

`BoardResponse` — `createdAt`은 `LocalDateTime.toString()` **문자열**이다.

페이징이 없다. 전체를 반환한다.

---

## POST `/api/board/new` — 게시판 생성

권한: 👑 `hasRole('ADMIN')`

### 요청

```json
{ "name": "질문게시판", "description": "궁금한 것을 물어보세요" }
```

| 필드 | 검증 |
|---|---|
| `name` | `@NotBlank` |
| `description` | 없음 (nullable) |

### 응답 `200 OK` — `BoardResponse`

> 생성인데 201이 아니라 **200**이다.

### 에러

| 상태 | code | 조건 |
|---|---|---|
| 400 | `INVALID_INPUT` | `name` 누락 |
| 401 | `LOGIN_REQUIRED` | 미인증 |
| 403 | `ACCESS_DENIED` | ADMIN 아님 |
| 409 | `DUPLICATE_BOARD_NAME` | 이름 중복 |

---

## PUT `/api/board/{id}/update` — 게시판 수정

권한: 👑 ADMIN

### 요청

```json
{ "name": "자유게시판(수정)", "description": "설명 변경" }
```

⚠️ 컨트롤러에 `@Valid`가 **없다** → `name`이 빈 문자열이어도 통과한다.
⚠️ 이름 중복 검사를 하지 않는다.

### 응답 `200 OK` — `BoardResponse`

### 에러

| 상태 | code |
|---|---|
| 403 | `ACCESS_DENIED` |
| 404 | `BOARD_NOT_FOUND` |

---

## DELETE `/api/board/{id}` — 게시판 삭제

권한: 👑 ADMIN

### 응답 `200 OK`

```
ok
```

⚠️ JSON이 아니라 **plain text** `"ok"` 문자열이다 (`ResponseEntity<String>`).
다른 엔드포인트와 응답 형식이 다르니 클라이언트에서 `res.json()` 호출 금지.

### 에러

| 상태 | code | 조건 |
|---|---|---|
| 403 | `ACCESS_DENIED` | ADMIN 아님 |
| 404 | `BOARD_NOT_FOUND` | 없는 ID |
| 400/500 | `SQL_INTEGRITY_ERROR` 등 | 하위 게시글이 있으면 FK 위반 |

> 게시글이 있는 게시판은 삭제할 수 없다. cascade가 없다.
> `ErrorCode.INVALID_BOARD_ID`가 이런 상황을 위해 정의됐지만 실제로 쓰이지 않는다.

---

## 프론트엔드 참고

- ADMIN 여부를 응답으로 알 수 없다 (`UserResponse.role`이 항상 null) → 관리 버튼 노출 판단이 어렵다.
  임시 방편: 관리 API를 호출해보고 403이면 숨기거나, 별도 관리자 페이지로 분리. → [[알려진-이슈]]
- 게시판 목록은 자주 바뀌지 않으므로 클라이언트에서 캐싱해도 좋다.

## 관련 문서

- [[엔티티-Board]]
- [[API-Post]]
- [[화면-정의서]]
