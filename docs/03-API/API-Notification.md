---
title: API Notification
tags: [api, notification]
updated: 2026-08-01
---

# API: 알림 (`/api/notify`)

`notification/NotificationController.java` · 전부 🔒 인증 필요

---

## GET `/api/notify/list` — 알림 목록

### 쿼리 파라미터

| 파라미터 | 기본값 |
|---|---|
| `page` | 0 |
| `size` | 10 |
| `sort` | `createdAt,DESC` (최신순) |

### 응답 `200 OK` — `Page<NotificationResponse>`

```json
{
  "content": [
    {
      "id": 31,
      "type": "COMMENT_ON_POST",
      "message": "김철수님이 게시글에 댓글을 달았습니다",
      "actorUsername": "김철수",
      "postId": 12,
      "commentId": 45,
      "read": false,
      "createdAt": "2026-07-30T21:20:11.005"
    }
  ],
  "page": { "size": 10, "number": 0, "totalElements": 5, "totalPages": 1 }
}
```

| 필드 | 설명 |
|---|---|
| `type` | `COMMENT_ON_POST` \| `REPLY_ON_COMMENT` |
| `message` | **서버가 조립한 한국어 문장**. 그대로 표시하면 된다 |
| `actorUsername` | 알림을 일으킨 사람의 닉네임 |
| `postId` | 이동할 게시글 ID (항상 존재) |
| `commentId` | 해당 댓글 ID (nullable) |
| `read` | 읽음 여부 |

로그인한 사용자가 **수신자(recipient)** 인 알림만 반환된다.

> `postId`/`commentId`는 FK가 아니다 → 대상 게시글이 삭제됐어도 알림은 남는다.
> 클릭 시 404가 날 수 있으므로 프론트엔드에서 처리 필요.

### 다국어가 필요하면

`message`를 쓰지 말고 `type` + `actorUsername`으로 클라이언트에서 조립한다.

```ts
const text = type === 'REPLY_ON_COMMENT'
  ? t('notify.reply', { name: actorUsername })
  : t('notify.comment', { name: actorUsername });
```

---

## GET `/api/notify/unreads` — 안 읽은 개수

헤더 뱃지용.

### 응답 `200 OK`

```json
{ "count": 3 }
```

`NotificationController.UnreadCountResponse` record.

---

## PUT `/api/notify/{id}/read` — 읽음 처리

`{id}`는 **알림 ID**.

### 응답 `200 OK`

바디 없음.

### 에러

| 상태 | code | 조건 |
|---|---|---|
| 401 | `LOGIN_REQUIRED` | 미인증 |
| 403 | `CANNOT_VIEW_NOTIFICATION` | 내 알림이 아님 |
| 404 | `COMMENT_NOT_FOUND` | 알림 없음 — ⚠️ ErrorCode가 부정확 ("댓글을 찾을 수 없습니다") |

---

## 알림 생성 방법

**알림을 만드는 API는 없다.** 댓글 작성 시 서버 내부 이벤트로만 생성된다.

| 트리거 | 알림 종류 | 수신자 |
|---|---|---|
| 게시글에 최상위 댓글 | `COMMENT_ON_POST` | 게시글 작성자 |
| 댓글에 대댓글 | `REPLY_ON_COMMENT` | 부모 댓글 작성자 |

자기 자신에게는 생성되지 않는다. → [[알림-이벤트-아키텍처]]

---

## 미구현

| 기능 | 상태 |
|---|---|
| 전체 읽음 처리 | ❌ 개별 `PUT`을 반복 호출해야 함 |
| 알림 삭제 | ❌ |
| 읽지 않은 알림만 필터 | ❌ (클라이언트에서 `read` 필드로 필터) |
| 좋아요 알림 | ❌ |
| **실시간 푸시 (SSE/WebSocket)** | ❌ |

### 실시간성 확보 방법

현재 서버가 푸시를 지원하지 않으므로 **폴링**만 가능하다.

```ts
// 30초 주기로 안 읽은 개수 확인
useEffect(() => {
  const id = setInterval(() => fetchUnreadCount(), 30_000);
  return () => clearInterval(id);
}, []);
```

`/api/notify/unreads`는 `COUNT` 쿼리 하나뿐이라 폴링 비용이 작다. 목록은 드롭다운을 열 때만 조회한다.

→ [[프론트엔드-요구사항]] FR-8

## 관련 문서

- [[엔티티-Notification]]
- [[알림-이벤트-아키텍처]]
- [[API-Comment]]
- [[API-페이지네이션]]
