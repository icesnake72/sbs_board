---
title: 엔티티 PostImage
tags: [domain, entity, post, file]
updated: 2026-08-01
---

# 엔티티: PostImage

`global/entity/PostImage.java` → 테이블 `post_images`

게시글에 첨부된 이미지의 메타데이터. 실제 바이너리는 디스크에 있다.

## 필드

| 필드 | 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| `id` | `id` | Long | PK | |
| `post` | `post_id` | `@ManyToOne(LAZY)` | NOT NULL | |
| `storedName` | `stored_name` | String | **NOT NULL** | 디스크 실제 파일명 `{uuid32}.{ext}` |
| `originalName` | `original_name` | String | nullable | 사용자가 올린 원본 파일명 |
| `contentType` | `content_type` | String | nullable | `image/png` 등 |
| `size` | `size` | long | | 바이트 |
| `sortOrder` | `sort_order` | int | NOT NULL | 0부터. 업로드 순서 |
| `createdAt` | `created_at` | LocalDateTime | 기본 `now()` | |

> `@Setter`가 없다 — `@Builder`로만 생성한다.
> `@EntityListeners`가 없고 `updatedAt` 필드도 없다.

## 도메인 메서드

```java
void assignPost(Post post)   // Post.addImage()에서 호출되는 양방향 연결
```

## DTO 변환

```java
private static final String URL_PREFIX = "/images/";

PostImage.toDto(image) → PostImageResponse {
    id,
    url,            // "/images/" + storedName   ← 클라이언트가 바로 쓰는 값
    originalName,
    sortOrder
}
```

`contentType`과 `size`는 **응답에 포함되지 않는다**.

## Post와의 관계

`Post` 쪽 정의:

```java
@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("sortOrder asc")
@BatchSize(size = 100)
private List<PostImage> images = new ArrayList<>();
```

| 설정 | 효과 |
|---|---|
| `cascade = ALL` | Post 저장 시 이미지 함께 INSERT |
| `orphanRemoval` | 리스트에서 제거하면 DELETE |
| `@OrderBy("sortOrder asc")` | 항상 업로드 순서대로 조회 |
| `@BatchSize(100)` | 여러 게시글 조회 시 이미지 IN 절로 묶어서 로딩 (N+1 완화) |

## 생성 흐름

`PostService.create()` 내부에서만 만들어진다. 독립적인 이미지 업로드 API는 없다.

```java
PostImage.builder()
    .storedName(fileStorageService.store(file))
    .originalName(file.getOriginalFilename())
    .contentType(file.getContentType())
    .size(file.getSize())
    .sortOrder(order++)
    .build();
```

## 리포지토리

**전용 리포지토리가 없다.** Post를 통해서만 접근한다.

## 미구현

- 이미지 개별 삭제 API
- 게시글 수정 시 이미지 추가/삭제/순서 변경
- 게시글 삭제 시 디스크 파일 정리

→ [[파일-업로드-아키텍처]], [[알려진-이슈]]

## 관련 문서

- [[엔티티-Post]]
- [[API-Post]]
