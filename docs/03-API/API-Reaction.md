---
title: API Reaction
tags: [api, reaction]
updated: 2026-08-01
---

# API: 반응 (좋아요 / 싫어요)

`reaction/ReactionController.java`

게시글·댓글 모두 반응 등록과 조회가 동작한다.
반응 상태는 **조회 API 응답에 함께 포함**되므로 별도 조회가 필요 없다.

- 게시글 → `PostDTO.like` / `disLike` / `myReaction` ([[API-Post]])
- 댓글 → `CommentResponse.likeCount` / `dislikeCount` / `myReaction` ([[API-Comment]])

---

## POST `/api/post/{postId}/reation` — 게시글 반응 토글

권한: 🔒 인증 필요

> 🚨 **경로 오타 주의**: `reaction`이 아니라 **`reation`** 이다.
> `@RequestMapping("/api/")` + `@PostMapping("/post/{postId}/reation")` 조합.

### 요청

```json
{ "type": "LIKE" }
```

| 필드 | 타입 | 검증 |
|---|---|---|
| `type` | `"LIKE"` \| `"DISLIKE"` | `@NotNull` |

### 응답 `200 OK`

```json
{ "likeCount": 12, "dislikeCount": 1, "myReaction": "LIKE" }
```

| 필드 | 설명 |
|---|---|
| `likeCount` | 이 게시글의 LIKE 총 개수 |
| `dislikeCount` | DISLIKE 총 개수 |
| `myReaction` | 요청자의 현재 반응. 취소했으면 **null** |

### 토글 규칙

| 현재 상태 | 요청 `type` | 결과 | `myReaction` |
|---|---|---|---|
| 없음 | LIKE | LIKE 생성 | `"LIKE"` |
| LIKE | LIKE | **취소 (삭제)** | `null` |
| LIKE | DISLIKE | DISLIKE로 전환 | `"DISLIKE"` |
| DISLIKE | LIKE | LIKE로 전환 | `"LIKE"` |

같은 버튼을 다시 누르면 취소된다는 점을 UI에서 반영할 것.

### 에러

| 상태 | code | 조건 |
|---|---|---|
| 400 | `INVALID_INPUT` | `type`이 null 또는 유효하지 않은 값 |
| 401 | `LOGIN_REQUIRED` | 미인증 |
| 404 | `POST_NOT_FOUND` | 없는 게시글 |

---

## POST `/api/comment/{commentId}/reaction` — 댓글 반응 토글

권한: 🔒 인증 필요

> 경로 철자는 **`reaction`**(정상)이다. 게시글 쪽 `reation` 오타와 다르니 주의.

### 요청

```json
{ "type": "LIKE" }
```

게시글 반응과 동일한 `ReactionRequest` record를 쓴다.

### 응답 `200 OK`

```json
{ "likeCount": 5, "dislikeCount": 0, "myReaction": "LIKE" }
```

`ReactionResponse` — 게시글 반응과 **동일한 DTO**다.

### 토글 규칙

게시글과 동일하다.

| 현재 상태 | 요청 `type` | 결과 | `myReaction` |
|---|---|---|---|
| 없음 | LIKE | LIKE 생성 | `"LIKE"` |
| LIKE | LIKE | **취소 (삭제)** | `null` |
| LIKE | DISLIKE | DISLIKE로 전환 | `"DISLIKE"` |
| DISLIKE | LIKE | LIKE로 전환 | `"LIKE"` |

`comment_reaction`의 `uk_comment_reaction (comment_id, user_id)` 제약으로 사용자당 댓글당 반응 1개가 보장된다.

### 에러

| 상태 | code | 조건 |
|---|---|---|
| 400 | `INVALID_INPUT` | `type`이 null 또는 유효하지 않은 값 |
| 401 | `LOGIN_REQUIRED` | 미인증 |
| 404 | `COMMENT_NOT_FOUND` | 없는 댓글 |

### 구현 참고

`ReactionService.reactToComment()` / `buildCommentReactionResponse()` — 게시글 로직을 그대로 대응시킨 구조다.
존재 확인은 `commentRepository.existsById()`, 연관 엔티티는 `getReferenceById()` 프록시로 참조해 불필요한 SELECT를 피한다.

> ⚠️ **삭제된(soft delete) 댓글에도 반응을 남길 수 있다.** 게시글 로직을 그대로 옮긴 결과이며,
> 존재 여부만 검사하고 `deleted` 플래그는 보지 않는다. 차단이 필요하면 별도 `ErrorCode` 추가가 필요하다.

---

## 구현 상태

| 기능 | 상태 | 비고 |
|---|---|---|
| 게시글 반응 등록/취소/전환 | ✅ | 경로 오타 `reation` 존재 |
| 댓글 반응 등록/취소/전환 | ✅ | `POST /api/comment/{commentId}/reaction` |
| 게시글 조회 시 반응 표시 | ✅ | 상세 + 목록 2종 모두 |
| 댓글 조회 시 반응 표시 | ✅ | 목록 + 수정 응답 |
| N+1 방지 배치 집계 | ✅ | 대상 수와 무관하게 쿼리 2회 |
| 삭제된 댓글 반응 차단 | ❌ | 존재 여부만 검사 → [[알려진-이슈]] BE-06b |
| 반응 알림 | ❌ | 미구현 |
| 반응한 사용자 목록 조회 | ❌ | 미구현 |

### 프론트엔드 대응

조회 응답에 반응 상태가 이미 포함되므로 **추가 요청이 필요 없다.**

- 진입 시: 게시글/댓글 조회 응답의 `like`/`disLike`/`myReaction`을 그대로 렌더링
- 클릭 시: 반응 API 응답(`ReactionResponse`)으로 해당 항목만 갱신
- `myReaction`을 받으려면 **조회 요청에도 `Authorization` 헤더**를 실을 것

주의: 토글이므로 연타하면 상태가 뒤집힌다. 요청 중에는 버튼을 비활성화한다.

---

## 배치 집계 (N+1 방지)

목록 조회에서 대상마다 개수를 세면 `대상 수 x 3` 쿼리가 발생한다.
이를 피하기 위해 `GROUP BY`로 한 번에 집계한다.

```java
// ReactionService
public Map<Long, ReactionResponse> summarizePostReactions(Collection<Long> postIds, Long userId)
public Map<Long, ReactionResponse> summarizeCommentReactions(Collection<Long> commentIds, Long userId)
```

두 메서드 모두 쿼리 **2회**만 실행하고 공통 `mergeSummaries()`로 결과를 합친다.

| 쿼리 | 내용 |
|---|---|
| 1 | `SELECT target_id, type, COUNT(*) ... WHERE target_id IN (...) GROUP BY target_id, type` |
| 2 | `SELECT target_id, type ... WHERE target_id IN (...) AND user_id = ?` — 비로그인이면 **생략** |

반환 맵은 요청한 모든 id를 key로 가지며, 반응이 없는 대상은 `0 / 0 / null`로 채워진다.

### 실측 쿼리 수

| 요청 | 전체 SQL | 반응 관련 |
|---|---|---|
| 댓글 6개(최상위 3 + 대댓글 3) 목록 | 6 | **2** |
| 게시글 4개 목록 | 5 | **2** (`IN (?,?,?,?)`) |

대상 개수가 늘어도 반응 관련 쿼리는 2회로 고정된다.

### 단건 조회용

`buildPostReactionResponse()` / `buildCommentReactionResponse()`는 반응 토글 직후 응답을 만들 때 쓴다
(카운트 2회 + 본인 반응 1회 = 3쿼리). 목록에는 사용하지 말 것.

## 관련 문서

- [[엔티티-Reaction]]
- [[API-Post]] · [[API-Comment]]
- [[프론트엔드-요구사항]] FR-7
