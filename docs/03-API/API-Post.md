---
title: API Post
tags: [api, post, upload]
updated: 2026-08-01
---

# API: 게시글 (`/api/post`)

`post/PostController.java`

---

## 공통 응답: PostDTO

```json
{
  "id": 12,
  "title": "안녕하세요",
  "author": "홍길동",
  "board": "자유게시판",
  "body": "첫 글입니다.",
  "viewCount": 42,
  "images": [
    { "id": 3, "url": "/images/3f2a9c1b4d5e6f708192a3b4c5d6e7f8.png", "originalName": "photo.png", "sortOrder": 0 }
  ],
  "like": 12,
  "disLike": 1,
  "myReaction": "LIKE",
  "createdAt": "2026-07-30T21:15:03.412",
  "canEdit": true,
  "canDelete": true
}
```

| 필드 | 주의사항 |
|---|---|
| `author` | 닉네임 **문자열**. 사용자 ID가 아니다 |
| `board` | 게시판 **이름 문자열**. boardId가 아니다 → 게시판으로 되돌아가는 링크를 만들 수 없다 |
| `createdAt` | ISO 문자열 (`LocalDateTime.toString()`) |
| `like` / `disLike` | 좋아요·싫어요 개수. **상세·목록 모두** 채워진다 |
| `myReaction` | `"LIKE"` \| `"DISLIKE"` \| `null`. **비로그인이거나 반응하지 않았으면 null** |
| `canEdit`/`canDelete` | 목록 API에서는 **항상 false**. 상세 API에서만 실제 소유 여부 반영 |
| `images[].url` | `/images/` 접두사 포함. `${API_BASE}${url}` 로 사용 |

> `myReaction`을 받으려면 조회 시에도 `Authorization` 헤더를 보내야 한다.
> `like`/`disLike`는 비로그인에서도 정상적으로 내려온다.
> 반응 조회는 **배치 집계**라 게시글이 몇 개든 관련 쿼리는 2회 고정이다. → [[API-Reaction]]

---

## GET `/api/post/all` — 전체 게시글

권한: 🌐 공개 (로그인 시 `myReaction` 개인화)

### 응답 `200 OK`

`List<PostDTO>` — **페이징 없음. 본문 전체 포함.**

⚠️ 게시글이 많아지면 응답이 매우 커진다. 프로덕션 화면에서는 쓰지 말고
게시판별 목록(`/api/post/{boardId}/all`)을 사용할 것.

`canEdit`/`canDelete`는 항상 false.

---

## GET `/api/post/{boardId}/all` — 게시판별 목록 (페이징)

권한: 🌐 공개 (로그인 시 `myReaction` 개인화)

### 쿼리 파라미터

| 파라미터 | 기본값 | 예시 |
|---|---|---|
| `page` | 0 | `?page=2` |
| `size` | **10** | `?size=20` |
| `sort` | `createdAt,DESC` | `?sort=viewCount,DESC` |

### 응답 `200 OK` — `Page<PostDTO>`

```json
{
  "content": [ /* PostDTO[] */ ],
  "page": { "size": 10, "number": 0, "totalElements": 37, "totalPages": 4 }
}
```

스키마 상세는 [[API-페이지네이션]].

`@EntityGraph({"board","author"})`로 조인 로딩되어 N+1이 없다.

> 존재하지 않는 `boardId`를 넘겨도 **404가 아니라 빈 페이지**가 반환된다 (게시판 존재 검증 코드가 주석 처리됨).

---

## GET `/api/post/{id}` — 게시글 상세

권한: 🌐 공개 (로그인 시 개인화)

### 동작

| 요청자 | `canEdit`/`canDelete` | 조회수 | `myReaction` |
|---|---|---|---|
| 비로그인 | false | 증가 안 함 | 항상 null |
| 로그인, 타인 글 | false | **+1** | 본인 반응 |
| 로그인, 본인 글 | **true** | 증가 안 함 | 본인 반응 |

로그인 상태는 `Authorization` 헤더 유무로 결정된다 (`@AuthenticationPrincipal`이 null이면 익명).

### 응답 `200 OK` — `PostDTO`

### 에러

| 상태 | code |
|---|---|
| 404 | `POST_NOT_FOUND` |

> 경로 충돌 주의: `/api/post/all`은 리터럴 매칭이 우선하므로 `{id}`로 잡히지 않는다.

---

## POST `/api/post/{boardId}/new` — 게시글 작성

권한: 🔒 인증 필요 (게시판 무관, 모든 로그인 사용자)

### 요청 — `multipart/form-data`

| Part | Content-Type | 필수 | 내용 |
|---|---|---|---|
| `post` | `application/json` | ✅ | `PostRequest` |
| `images` | `image/*` | ❌ | 파일. 여러 개 가능 (최대 5) |

```json
// part "post"
{ "title": "제목입니다", "body": "본문 내용" }
```

| 필드 | 검증 |
|---|---|
| `title` | `@NotBlank @Size(min=5, max=200)` — **최소 5자** |
| `body` | `@NotBlank` |

### JavaScript 예시

```js
const fd = new FormData();
fd.append('post', new Blob([JSON.stringify({ title, body })], { type: 'application/json' }));
files.forEach(f => fd.append('images', f));

await fetch(`${API_BASE}/api/post/${boardId}/new`, {
  method: 'POST',
  headers: { Authorization: accessToken },  // Content-Type은 설정하지 말 것 (boundary 자동)
  body: fd,
});
```

> `post` 파트를 `Blob` + `application/json`으로 감싸지 않으면 Spring이 `PostRequest`로 역직렬화하지 못한다.

### 이미지 제약

| 항목 | 값 | 위반 시 |
|---|---|---|
| 개수 | ≤ 5 | 400 `FILE_COUNT_EXCEEDED` |
| 파일당 크기 | ≤ 2MB | 400 `MAX_UPLOAD_SIZE_EXCEEDED` |
| 요청 전체 | ≤ 20MB | 400 `MAX_UPLOAD_SIZE_EXCEEDED` |
| MIME | `image/png,jpeg,gif,webp` | 400 `INVALID_FILE_TYPE` |
| 확장자 | `png,jpg,jpeg,gif,webp` | 400 `INVALID_FILE_TYPE` |

정렬 순서(`sortOrder`)는 전송 순서대로 0부터 부여된다.
→ [[파일-업로드-아키텍처]]

### 응답 `200 OK` — `PostDTO`

> 생성인데 201이 아니라 200.
> `canEdit`/`canDelete`는 `Post.toDTO(post)` (1인자 버전)를 쓰므로 **false**로 내려온다.

### 에러

| 상태 | code | 조건 |
|---|---|---|
| 400 | `INVALID_INPUT` | 제목 5자 미만 등 |
| 400 | `FILE_COUNT_EXCEEDED` / `INVALID_FILE_TYPE` / `MAX_UPLOAD_SIZE_EXCEEDED` | |
| 401 | `LOGIN_REQUIRED` | 미인증 |
| 404 | `BOARD_NOT_FOUND` / `USER_NOT_FOUND` | |
| 500 | `FILE_UPLOAD_FAILED` | 디스크 I/O 실패 |

실패 시 이미 저장된 파일은 자동 삭제된다 (보상 처리).

---

## PUT `/api/post/{id}/update` — 게시글 수정

권한: ✍️ **작성자 본인만** — `@PreAuthorize("@postSecurity.isAuthor(#id, authentication.principal)")`

### 요청 — `application/json`

```json
{ "title": "수정된 제목", "body": "수정된 본문" }
```

`PostRequest` — 생성과 동일한 검증(`title` 5~200자, `body` 필수).

### 응답 `200 OK` — `PostDTO`

### 에러

| 상태 | code | 조건 |
|---|---|---|
| 400 | `INVALID_INPUT` | |
| 401 | `LOGIN_REQUIRED` | |
| 403 | `ACCESS_DENIED` | 작성자 아님 (ADMIN도 불가) |
| 404 | `POST_NOT_FOUND` | |

⚠️ **이미지는 수정할 수 없다.** title/body만 변경된다. → [[알려진-이슈]]

---

## DELETE `/api/post/{id}` — 게시글 삭제

권한: ✍️ 작성자 본인만

### 응답 `200 OK`

```
ok
```

plain text. `res.json()` 호출 금지.

### 에러

| 상태 | code |
|---|---|
| 403 | `ACCESS_DENIED` |
| 404 | `POST_NOT_FOUND` |

DB의 `post_images` 레코드는 cascade 삭제되지만 **디스크 파일은 남는다**.

> 게시글에 댓글이 달려 있으면 `comments.post_id` FK 제약으로 삭제가 실패할 수 있다
> (`Post`에 comments cascade 설정이 없다).

---

## 관련 문서

- [[엔티티-Post]] · [[엔티티-PostImage]]
- [[파일-업로드-아키텍처]]
- [[API-Comment]] · [[API-Reaction]]
- [[API-페이지네이션]]
