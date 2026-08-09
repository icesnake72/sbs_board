---
title: 엔티티 Post
tags: [domain, entity, post]
updated: 2026-08-01
---

# 엔티티: Post

`global/entity/Post.java` → 테이블 `posts`

## 필드

| 필드 | 컬럼 | 타입 | 제약 |
|---|---|---|---|
| `id` | `id` | Long | PK |
| `title` | `title` | String(200) | **NOT NULL** |
| `body` | `body` | TEXT | nullable(DB) / `@NotBlank`(요청 DTO) |
| `board` | `board_id` | `@ManyToOne(LAZY)` | NOT NULL |
| `author` | `user_id` | `@ManyToOne(LAZY)` | NOT NULL |
| `images` | — | `@OneToMany(mappedBy="post")` | cascade ALL, orphanRemoval, `@OrderBy("sortOrder asc")`, `@BatchSize(100)` |
| `viewCount` | `view_count` | long | NOT NULL, 기본 0 |
| `createdAt` | `created_at` | LocalDateTime | 기본 `now()` |
| `updatedAt` | `updated_at` | LocalDateTime | `@LastModifiedDate` (`@EntityListeners` ✅) |

> 필드명은 `author`지만 컬럼명은 `user_id`다.

## 도메인 메서드

```java
boolean isAuthor(Long userId)      // author.getId().equals(userId)
void addImage(PostImage image)     // 양방향 편의 메서드 (image.assignPost(this))
void increaseViewCount()           // viewCount++
```

## 조회수 정책

`PostService.getPost()`

```java
if (!post.isAuthor(loginUserId) && loginUserId != null) {
    post.increaseViewCount();
}
```

| 상황 | 증가 |
|---|---|
| 비로그인 조회 | ❌ 증가 안 함 |
| 로그인, 본인 글 | ❌ |
| 로그인, 타인 글 | ✅ 매 요청마다 증가 (중복 방지 없음) |

새로고침할 때마다 증가하므로 실제 "조회수"라기보다 "요청 횟수"에 가깝다.

## DTO 변환 — 오버로드 2개

```java
Post.toDTO(post, loginUserId)   // canEdit/canDelete를 소유 여부로 계산
Post.toDTO(post)                // canEdit/canDelete 항상 false
```

`PostDTO` 필드

| 필드 | 내용 |
|---|---|
| `id`, `title`, `body` | |
| `author` | 작성자 **닉네임 문자열** (id 아님) |
| `board` | 게시판 **이름 문자열** (id 아님) |
| `viewCount` | |
| `images` | `List<PostImageResponse>` |
| `like`, `disLike`, `myReaction` | 반응 집계. `PostService`가 배치 조회해 채운다 → [[API-Reaction]] |
| `createdAt` | `String` (LocalDateTime.toString) |
| `canEdit`, `canDelete` | 소유권 힌트 — UI 버튼 노출 제어용 |

> `author`/`board`가 ID가 아니라 이름 문자열이라 프론트엔드에서 **작성자 프로필이나 게시판으로 이동하는 링크를 만들 수 없다.** [[알려진-이슈]] BE-10 참조.

### DTO 변환 시 반응 채우기

`Post.toDTO()`는 반응을 다루지 않는다. `PostService`가 변환 후 `applyReaction()`으로 채운다.

```java
Map<Long, ReactionResponse> summaries =
        reactionService.summarizePostReactions(postIds, loginUserId);
return posts.map(post -> applyReaction(Post.toDTO(post), post.getId(), summaries));
```

목록·상세 모두 이 경로를 쓰며, 게시글이 몇 개든 반응 관련 쿼리는 2회다.

`PostListResponse`(`{id, title, author, viewCount, thumbnailUrl, createdAt}`) 클래스가 정의돼 있으나 **미사용**이다. 목록 조회도 전부 `PostDTO`(본문 포함)를 반환한다.

## 리포지토리 쿼리

`post/PostRepository.java`

| 메서드 | 특징 |
|---|---|
| `findByBoard(Board)` | 미사용 |
| `findByBoardId(Long, Pageable)` | `@EntityGraph({"board","author"})` — N+1 방지 |
| `findDetailById(Long)` | `join fetch` board/author + `left join fetch` images. **정의돼 있으나 미사용** |
| `findAuthorIdById(Long)` | 작성자 ID만 조회. `PostSecurity`와 알림 리스너에서 사용 |

> `getPost()`는 `findDetailById` 대신 기본 `findById`를 쓴다 → 이미지 로딩 시 추가 쿼리 발생.
> 단 `@BatchSize(100)` 덕에 N+1은 완화된다.

## 소유권 / 권한

| 작업 | 권한 |
|---|---|
| 조회 (`GET`) | 누구나 |
| 생성 | 인증된 사용자 (게시판 무관) |
| 수정 / 삭제 | **작성자 본인만** — `@PreAuthorize("@postSecurity.isAuthor(#id, authentication.principal)")` |

> ADMIN이라도 남의 글을 수정/삭제할 수 없다.
> `PostService` 안의 `validateAuthor()`는 주석 처리되어 미사용.

## 삭제

Hard delete. `post_images` 레코드는 cascade로 삭제되지만 **디스크 파일은 남는다**.
→ [[파일-업로드-아키텍처]]

## 관련 문서

- [[API-Post]]
- [[엔티티-PostImage]] · [[엔티티-Comment]] · [[엔티티-Reaction]]
- [[엔티티-Board]]
