---
title: API Comment
tags: [api, comment]
updated: 2026-08-01
---

# API: 댓글 (`/api/comment`)

`comment/CommentController.java`

2단계 구조: 최상위 댓글 + 대댓글. → [[엔티티-Comment]]

---

## 공통 응답: CommentResponse

```json
{
  "id": 7,
  "authorUsername": "홍길동",
  "content": "좋은 글이네요",
  "deleted": false,
  "createdAt": "2026-07-30T21:20:11.005",
  "likeCount": 3,
  "dislikeCount": 0,
  "myReaction": "LIKE",
  "children": [
    {
      "id": 9,
      "authorUsername": "김철수",
      "content": "동의합니다",
      "deleted": false,
      "createdAt": "2026-07-30T21:25:00.100",
      "likeCount": 0,
      "dislikeCount": 0,
      "myReaction": null,
      "children": []
    }
  ]
}
```

| 필드 | 주의사항 |
|---|---|
| `authorUsername` | 닉네임 문자열. **사용자 ID가 없다** → 내 댓글 판별 불가 |
| `content` | `deleted: true`면 `"삭제된 게시글입니다"`로 치환 |
| `likeCount` / `dislikeCount` | 해당 댓글의 반응 개수. 대댓글에도 각각 집계된다 |
| `myReaction` | `"LIKE"` \| `"DISLIKE"` \| `null`. **비로그인이거나 반응하지 않았으면 null** |
| `children` | 최상위 댓글에만 채워짐. 대댓글은 항상 `[]` |
| — | `parentId` 필드가 **없다** (중첩 구조로만 표현) |

> `myReaction`을 받으려면 조회 시에도 `Authorization` 헤더를 보내야 한다.
> 반응 개수(`likeCount`/`dislikeCount`)는 비로그인에서도 정상적으로 내려온다.

---

## GET `/api/comment/post/{postId}/list` — 댓글 목록

권한: 🌐 공개 (로그인 시 `myReaction` 개인화)

### 쿼리 파라미터

| 파라미터 | 기본값 |
|---|---|
| `page` | 0 |
| `size` | 10 |
| `sort` | `createdAt,DESC` |

### 응답 `200 OK` — `Page<CommentResponse>`

```json
{
  "content": [ /* CommentResponse[] (최상위 댓글만) */ ],
  "page": { "size": 10, "number": 0, "totalElements": 23, "totalPages": 3 }
}
```

**페이징 단위는 최상위 댓글**이다. 대댓글은 각 항목의 `children`에 전부 포함되어 함께 내려온다 (대댓글에는 페이징이 없다).
따라서 `totalElements`는 전체 댓글 수가 아니라 최상위 댓글 수다.

기본 정렬이 `createdAt DESC` → 최신 댓글이 위. 게시판 관례상 오래된 순이 자연스러우면 `?sort=createdAt,ASC`를 명시할 것.
(대댓글은 항상 `createdAt ASC` 고정 — `@OrderBy`)

### 반응 정보 조회 비용

반응은 **배치 집계**로 가져온다. 댓글이 몇 개든 반응 관련 쿼리는 **2회 고정**(비로그인 1회)이다.

| 요청 | 전체 SQL |
|---|---|
| 댓글 6개(최상위 3 + 대댓글 3) 목록 | 6개 — 이 중 반응 관련 2개 |

`ReactionService.summarizeCommentReactions()` 참조. → [[API-Reaction]]

### 에러

| 상태 | code |
|---|---|
| 404 | `POST_NOT_FOUND` |

---

## POST `/api/comment/post/{postId}/new` — 댓글 / 대댓글 작성

권한: 🔒 인증 필요

### 요청

```json
// 최상위 댓글
{ "content": "좋은 글이네요" }

// 대댓글
{ "content": "동의합니다", "parentId": 7 }
```

| 필드 | 검증 |
|---|---|
| `content` | `@NotBlank @Size(max=1000)` |
| `parentId` | 선택. null이면 최상위 |

### 응답 `200 OK` — `CommentResponse`

> 대댓글로 생성한 경우, 응답의 `children`은 `[]`이다(자신은 대댓글이므로).
> 화면 갱신을 위해서는 목록을 다시 조회하는 편이 안전하다.

### 에러

| 상태 | code | 조건 |
|---|---|---|
| 400 | `INVALID_INPUT` | 내용 없음 / 1000자 초과 |
| 400 | `COMMENT_POST_MISMATCH` | 부모 댓글이 이 게시글의 댓글이 아님 |
| 400 | `CANNOT_REPLY_TO_DELETED` | 삭제된 댓글에 대댓글 시도 |
| 400 | `CANNOT_REPLY_TO_REPLY` | 대댓글에 다시 대댓글 시도 (깊이 2 제한) |
| 401 | `LOGIN_REQUIRED` | |
| 404 | `POST_NOT_FOUND` / `USER_NOT_FOUND` / `COMMENT_NOT_FOUND` | |

### 부가 동작: 알림 발행

댓글이 저장되고 **트랜잭션이 커밋된 후** 알림이 생성된다.

| `parentId` | 알림 종류 | 수신자 |
|---|---|---|
| null | `COMMENT_ON_POST` | 게시글 작성자 |
| 있음 | `REPLY_ON_COMMENT` | 부모 댓글 작성자 |

자기 자신에게는 알림이 가지 않는다. → [[알림-이벤트-아키텍처]]

---

## PUT `/api/comment/{id}` — 댓글 수정

권한: ✍️ 작성자 본인만 — `@PreAuthorize("@commentSecurity.isAuthor(#id, authentication.principal)")`

경로의 `{id}`는 **댓글 ID**다 (postId 아님).

### 요청

```json
{ "content": "수정된 내용" }
```

`parentId`를 보내도 무시된다 (부모는 변경 불가).

### 응답 `200 OK` — `CommentResponse`

기존 반응(`likeCount`/`dislikeCount`/`myReaction`)은 수정 후에도 그대로 유지되어 응답에 포함된다.

### 에러

| 상태 | code | 조건 |
|---|---|---|
| 400 | `INVALID_INPUT` | 내용 없음 / 1000자 초과 |
| 400 | `CANNOT_EDIT_DELETED` | **삭제된 댓글**을 수정하려 할 때 |
| 403 | `ACCESS_DENIED` | 작성자 아님 |
| 404 | `COMMENT_NOT_FOUND` | 없는 댓글 |

> 2026-08-01 이전에는 조건이 반전되어 정상 댓글 수정이 항상 400으로 실패했다(BE-03). 수정 완료.

---

## DELETE `/api/comment/{id}` — 댓글 삭제

권한: ✍️ 작성자 본인만

### 응답 `200 OK`

바디 없음 (`ResponseEntity<Void>`).

### 동작 — Soft Delete

레코드는 남고 `deleted = true`가 된다.
이후 조회 시 `content`가 `"삭제된 게시글입니다"`로 치환된다. **닉네임은 그대로 노출**된다.

대댓글은 함께 삭제되지 않는다 (트리 유지).
삭제된 댓글에는 새 대댓글을 달 수 없다 (`CANNOT_REPLY_TO_DELETED`).

### 에러

| 상태 | code |
|---|---|
| 403 | `ACCESS_DENIED` |
| 404 | `COMMENT_NOT_FOUND` |

---

## 프론트엔드 참고

1. **"내 댓글" 판별 불가** — 응답에 작성자 ID가 없다. 닉네임 문자열 비교가 유일한 방법인데 중복 닉네임 시 오작동한다.
   → 서버에 `authorId` 또는 `canEdit`/`canDelete` 필드 추가를 요청할 것. [[알려진-이슈]]
2. **낙관적 UI 주의** — 대댓글 생성 응답만으로는 트리를 재구성하기 어렵다. 작성 후 목록 재조회를 권장.
3. **삭제된 댓글** — `deleted: true`면 회색 처리하고 답글/수정 버튼을 숨긴다.
   반응 버튼도 숨기는 편이 좋다(서버는 차단하지 않는다 — [[알려진-이슈]] BE-06b).
4. **반응 표시** — `likeCount`/`dislikeCount`/`myReaction`이 목록 응답에 포함되므로 별도 조회가 필요 없다.
   토글은 `POST /api/comment/{id}/reaction`. → [[API-Reaction]]
5. **생성 응답의 반응은 항상 0** — 새 댓글이므로 정상이다.

## 관련 문서

- [[엔티티-Comment]]
- [[알림-이벤트-아키텍처]]
- [[API-페이지네이션]]
- [[화면-정의서]]
