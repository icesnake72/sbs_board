---
title: 엔티티 Board
tags: [domain, entity, board]
updated: 2026-08-01
---

# 엔티티: Board

`global/entity/Board.java` → 테이블 `boards`

게시글을 담는 최상위 분류 단위 (예: "자유게시판", "공지사항").

## 필드

| 필드 | 컬럼 | 타입 | 제약 |
|---|---|---|---|
| `id` | `id` | Long | PK |
| `name` | `name` | String(100) | **NOT NULL** (DB UNIQUE 제약은 없음) |
| `description` | `description` | TEXT | nullable |
| `createdAt` | `created_at` | LocalDateTime | 기본 `now()` |
| `updatedAt` | `updated_at` | LocalDateTime | `@LastModifiedDate` (`@EntityListeners` ✅) |

## 연관 관계

Board 쪽에는 `posts` 컬렉션이 **없다** (단방향).
`Post`가 `@ManyToOne board`로 참조한다. → [[엔티티-Post]]

조회는 `PostRepository.findByBoardId(boardId, pageable)`로 한다.

## DTO 변환

```java
Board.toDTO(board) → BoardResponse {
    id, name, description, createdAt(String)
}
```

`BoardDTO`(`{id, name}`)라는 클래스도 있으나 **현재 어떤 응답에도 쓰이지 않는다**.

## 이름 중복 정책

```java
if (boardRepository.existsByName(request.getName())) {
    throw new DuplicateException(DUPLICATE_BOARD_NAME);   // 409
}
```

- 애플리케이션 레벨 검사만 존재. **DB UNIQUE 제약이 없다**
- 동시 요청 시 같은 이름의 게시판이 2개 생길 수 있다
- 수정(`update`) 시에는 중복 검사를 **하지 않는다**

## 권한

`board/BoardController.java` — 조회를 제외한 모든 작업은 `@PreAuthorize("hasRole('ADMIN')")`

| 작업 | 권한 |
|---|---|
| `GET /api/board/all` | 누구나 |
| `POST /api/board/new` | ADMIN |
| `PUT /api/board/{id}/update` | ADMIN |
| `DELETE /api/board/{id}` | ADMIN |

`BoardService.validateUser(loginUserId)` 메서드가 남아 있으나 **호출되지 않는다**(`@PreAuthorize`로 대체됨).

## 삭제 시 주의

Hard delete. 하위 게시글이 있으면 FK 제약 위반 → `SQL_INTEGRITY_ERROR` 또는 500.
cascade가 설정되어 있지 않다.

`ErrorCode.INVALID_BOARD_ID`("해당 Board ID는 삭제할 수 없습니다.")가 정의돼 있으나 **사용처가 없다**.

## 리포지토리

`board/BoardRepository.java`

```java
boolean existsByName(String name);
// 나머지는 JpaRepository 기본 (findAll, findById, save, delete)
```

## 관련 문서

- [[API-Board]]
- [[엔티티-Post]]
- [[화면-정의서]] — 게시판 목록 화면
