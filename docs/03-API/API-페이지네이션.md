---
title: API 페이지네이션
tags: [api, pagination, reference]
updated: 2026-08-01
---

# 페이지네이션 규격

## 설정

`global/config/WebConfig.java`

```java
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
```

Spring Data의 `PageImpl` 직렬화 경고를 피하기 위해 **DTO 모드**를 쓴다.
→ 응답 스키마가 구버전(`content`, `pageable`, `totalElements`, ... 플랫 구조)과 **다르다**.

## 응답 스키마

```json
{
  "content": [ /* T[] */ ],
  "page": {
    "size": 10,
    "number": 0,
    "totalElements": 37,
    "totalPages": 4
  }
}
```

| 필드 | 타입 | 의미 |
|---|---|---|
| `content` | `T[]` | 현재 페이지 데이터 |
| `page.size` | number | 페이지 크기 |
| `page.number` | number | 현재 페이지 (**0부터 시작**) |
| `page.totalElements` | number | 전체 항목 수 |
| `page.totalPages` | number | 전체 페이지 수 |

> 구버전 Spring Data 응답에 있던 `first`, `last`, `numberOfElements`, `empty`, `sort` 등은 **없다**.
> `isLast`는 `page.number + 1 >= page.totalPages` 로 계산한다.

## TypeScript 타입

```ts
export interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export const isLastPage = (p: Page<unknown>) => p.page.number + 1 >= p.page.totalPages;
export const isFirstPage = (p: Page<unknown>) => p.page.number === 0;
```

## 요청 파라미터

Spring Data 표준 바인딩.

| 파라미터 | 형식 | 예시 |
|---|---|---|
| `page` | 0-based 정수 | `?page=2` |
| `size` | 정수 | `?size=20` |
| `sort` | `{property},{ASC\|DESC}` | `?sort=createdAt,DESC` |

정렬 다중 지정:

```
?sort=viewCount,DESC&sort=createdAt,DESC
```

`sort`의 property는 **엔티티 필드명**이다 (DTO 필드명이 아님).
잘못된 필드명을 주면 500이 난다 (`PropertyReferenceException`).

## 페이징을 지원하는 엔드포인트

| 엔드포인트 | 기본 size | 기본 sort | 항목 타입 |
|---|---|---|---|
| `GET /api/post/{boardId}/all` | 10 | `createdAt,DESC` | `PostDTO` |
| `GET /api/comment/post/{postId}/list` | 10 | `createdAt,DESC` | `CommentResponse` |
| `GET /api/notify/list` | 10 | `createdAt,DESC` | `NotificationResponse` |

기본값은 컨트롤러의 `@PageableDefault`로 지정되어 있다.

### 페이징이 **없는** 조회

| 엔드포인트 | 비고 |
|---|---|
| `GET /api/post/all` | ⚠️ 전체 게시글을 본문 포함해 한 번에 반환 |
| `GET /api/board/all` | 게시판은 수가 적어 문제 없음 |

## 정렬 가능한 필드

| 엔드포인트 | 사용 가능한 property |
|---|---|
| 게시글 | `id`, `title`, `viewCount`, `createdAt`, `updatedAt` |
| 댓글 | `id`, `content`, `createdAt`, `updatedAt` |
| 알림 | `id`, `createdAt`, `read` |

## 주의: 댓글 페이징 단위

`GET /api/comment/post/{postId}/list`는 **최상위 댓글만** 페이징한다.
대댓글은 각 항목의 `children` 배열에 전부 포함된다.

→ `page.totalElements`는 **전체 댓글 수가 아니라 최상위 댓글 수**다.
"댓글 23개" 같은 표시를 하려면 대댓글까지 세어야 한다 (현재 그런 API는 없다).

## 클라이언트 구현 예시

```ts
async function fetchPosts(boardId: number, page = 0, size = 10, sort = 'createdAt,DESC') {
  const qs = new URLSearchParams({ page: String(page), size: String(size), sort });
  const res = await api.get<Page<PostDTO>>(`/api/post/${boardId}/all?${qs}`);
  return res;
}
```

무한 스크롤을 쓴다면 `content`를 누적하고 `isLastPage()`로 종료를 판단한다.

## 관련 문서

- [[API-개요]]
- [[프론트엔드-데이터-모델]]
- [[API-클라이언트-가이드]]
