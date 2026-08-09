---
title: 엔티티 Notification
tags: [domain, entity, notification]
updated: 2026-08-01
---

# 엔티티: Notification

`notification/Notification.java` → 테이블 `notifications`

> `global/entity`가 아니라 `notification` 패키지에 있다.

## 필드

| 필드 | 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| `id` | `id` | Long | PK | |
| `recipient` | `recipient_id` | `@ManyToOne(LAZY)` User | NOT NULL | 알림을 **받는** 사람 |
| `actor` | `actor_id` | `@ManyToOne(LAZY)` User | NOT NULL | 알림을 **일으킨** 사람 |
| `notificationType` | `notification_type` | enum(STRING, 30) | NOT NULL | |
| `postId` | `post_id` | Long | **NOT NULL** | ⚠️ FK 아님, 단순 컬럼 |
| `commentId` | `comment_id` | Long | nullable | ⚠️ FK 아님 |
| `read` | `is_read` | boolean | NOT NULL, 기본 false | |
| `createdAt` | `created_at` | LocalDateTime | 기본 `now()` | |
| `updatedAt` | `updated_at` | LocalDateTime | `@LastModifiedDate` — ⚠️ `@EntityListeners` 없음 |

`postId`/`commentId`는 연관관계가 아닌 **원시 Long 컬럼**이다.
→ 게시글/댓글이 삭제되어도 알림은 남는다. 클릭 시 404가 날 수 있으니 프론트엔드에서 처리 필요.

## NotificationType

```java
public enum NotificationType {
    COMMENT_ON_POST,    // 게시글에 댓글 → 게시글 작성자에게
    REPLY_ON_COMMENT    // 댓글에 대댓글 → 댓글 작성자에게
}
```

## 도메인 메서드

```java
void markAsRead()                 // read = true
boolean isOwnedBy(Long userId)    // recipient.getId().equals(userId)
```

## 응답 변환

```java
Notification.toResponse(n) → NotificationResponse {
    id, type, message, actorUsername, postId, commentId, read, createdAt
}
```

`message`는 **서버에서 조립된 한국어 문장**이다.

```java
type == REPLY_ON_COMMENT
    ? actor.getNickName() + "님이 댓글에 댓글을 달았습니다"
    : actor.getNickName() + "님이 게시글에 댓글을 달았습니다"
```

## 생성

직접 호출되는 API가 없다. 오직 `NotificationEventListener`가 만든다.
→ [[알림-이벤트-아키텍처]]

```java
NotificationService.create(recipientId, actorId, type, postId, commentId)
```

**자기 자신에게는 생성되지 않는다.**

## 조회

`notification/NotificationRepository.java`

```java
@EntityGraph(attributePaths = {"actor"})     // 닉네임 조립 위해 actor 조인
Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

long countByRecipientIdAndReadIsFalse(Long recipientId);
```

## 읽음 처리

`NotificationService.read(notificationId, userId)`

```java
if (!notification.isOwnedBy(userId)) {
    throw new BusinessException(ErrorCode.CANNOT_VIEW_NOTIFICATION);   // 403
}
notification.markAsRead();
```

Dirty checking으로 UPDATE된다 (명시적 save 없음).

> 알림이 없을 때 던지는 ErrorCode가 `COMMENT_NOT_FOUND`다 — 의미상 부정확. [[알려진-이슈]]

## 미구현

- 전체 읽음 처리 (`PUT /api/notify/read-all`)
- 알림 삭제
- 반응(좋아요) 알림
- 실시간 푸시 (SSE/WebSocket) → 현재는 폴링만 가능

## 관련 문서

- [[API-Notification]]
- [[알림-이벤트-아키텍처]]
- [[엔티티-Comment]]
