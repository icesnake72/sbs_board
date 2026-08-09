---
title: API 개요
tags: [api, overview, index]
updated: 2026-08-01
---

# API 개요

- Base URL (로컬): `http://localhost:8090`
- 프리픽스: `/api`
- 인증: `Authorization: Bearer {accessToken}`
- Content-Type: `application/json;charset=UTF-8` (게시글 생성만 `multipart/form-data`)

## 전체 엔드포인트

### 인증 — [[API-Auth]]

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/auth/signup` | 🌐 | 회원가입 → 201 |
| POST | `/api/auth/login` | 🌐 | 로그인 → 토큰 + Refresh 쿠키 |
| POST | `/api/auth/logout` | 🌐 | Refresh 삭제 + 쿠키 만료 |
| POST | `/api/auth/reissue` | 🍪 | Access Token 재발급 |

### 소셜 로그인 — [[API-OAuth]]

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/oauth2/authorization/kakao` | 🌐 | **권장** 카카오 로그인 시작 |
| GET | `/oauth2/authorization/google` | 🌐 | **권장** 구글 로그인 시작 |
| GET | `/login/oauth2/code/{provider}` | 🌐 | 콜백 (Spring 내부 처리) |
| GET | `/api/oauth/kakao/login` | 🌐 | ⚠️ 레거시 수동 구현 |
| GET | `/api/oauth/kakao/callback` | 🌐 | ⚠️ 레거시 |

### 사용자 — [[API-User]]

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/user/me` | 🔒 | 내 프로필 — ⚪ **추후 구현 예정** |

### 게시판 — [[API-Board]]

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/board/all` | 🌐 | 게시판 전체 목록 |
| POST | `/api/board/new` | 👑 | 게시판 생성 |
| PUT | `/api/board/{id}/update` | 👑 | 게시판 수정 |
| DELETE | `/api/board/{id}` | 👑 | 게시판 삭제 |

### 게시글 — [[API-Post]]

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/post/all` | 🌐 | 전체 게시글 (페이징 없음, 비권장) |
| GET | `/api/post/{boardId}/all` | 🌐 | 게시판별 목록 (페이징) |
| GET | `/api/post/{id}` | 🌐 | 상세 (로그인 시 조회수 증가) |
| POST | `/api/post/{boardId}/new` | 🔒 | 작성 (multipart, 이미지 ≤5) |
| PUT | `/api/post/{id}/update` | ✍️ | 수정 (작성자만) |
| DELETE | `/api/post/{id}` | ✍️ | 삭제 (작성자만) |

### 댓글 — [[API-Comment]]

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/comment/post/{postId}/list` | 🌐 | 댓글 목록 (페이징, 대댓글 중첩) |
| POST | `/api/comment/post/{postId}/new` | 🔒 | 댓글/대댓글 작성 |
| PUT | `/api/comment/{id}` | ✍️ | 수정 (작성자만) |
| DELETE | `/api/comment/{id}` | ✍️ | Soft delete |

### 반응 — [[API-Reaction]]

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/post/{postId}/reation` | 🔒 | 게시글 좋아요/싫어요 토글 — ⚠️ **경로 오타** |
| POST | `/api/comment/{commentId}/reaction` | 🔒 | 댓글 좋아요/싫어요 토글 |

### 알림 — [[API-Notification]]

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/notify/list` | 🔒 | 알림 목록 (페이징) |
| GET | `/api/notify/unreads` | 🔒 | 안 읽은 개수 |
| PUT | `/api/notify/{id}/read` | 🔒 | 읽음 처리 |

### 정적 리소스

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | `/images/{storedName}` | 🌐 | 업로드 이미지 |

### 권한 범례

| 기호 | 의미 |
|---|---|
| 🌐 | 인증 불필요 (`permitAll`) |
| 🍪 | Refresh 쿠키 필요 |
| 🔒 | 인증 필요 (`Bearer` 토큰) |
| ✍️ | 작성자 본인만 (`@PreAuthorize` 소유권 검사) |
| 👑 | `ROLE_ADMIN` |

## 권한 규칙 (SecurityConfig)

```java
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/api/oauth/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/board/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/post/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/comment/**").permitAll()
.requestMatchers(HttpMethod.GET, "/images/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/user/me").authenticated()
.anyRequest().authenticated()
```

**GET은 대부분 공개, 나머지 메서드는 전부 인증 필요**가 기본 원칙이다.

## 공통 규약

### 요청 헤더

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

> 로그인 응답의 `accessToken`에는 이미 `"Bearer "` 접두사가 **포함**되어 있다.
> `/api/auth/reissue` 응답에는 **포함되어 있지 않다**. 헤더 조립 시 주의. → [[API-클라이언트-가이드]]

### 성공 응답

DTO를 그대로 반환한다. 공통 래퍼(`{data: ...}`)가 **없다**.

### 실패 응답 — 항상 동일한 형태

```json
{
  "code": "POST_NOT_FOUND",
  "message": "게시글을 찾을 수 없습니다.",
  "timestamp": "2026-08-01T14:32:11.123"
}
```

→ [[API-에러코드]]

### 페이징

`Page<T>` 반환 엔드포인트는 [[API-페이지네이션]]의 공통 스키마를 따른다.

## ⚠️ 프론트엔드 시작 전 반드시 확인

| 이슈 | 영향 |
|---|---|
| **CORS 미설정** (⏸ 보류) | 브라우저에서 다른 오리진의 모든 요청이 차단된다 → 개발 프록시로 우회 |
| Refresh 쿠키 `SameSite=Strict`, `Path=/api/auth` | 크로스 오리진에서 쿠키가 전송되지 않음 |
| `GET /api/user/me` (⚪ 추후 구현) | 프로필 화면은 이후 단계 |
| **`/error`가 permitAll 아님** | 미처리 예외가 401로 위장되어 강제 로그아웃을 유발할 수 있음 |

→ [[알려진-이슈]], [[프론트엔드-요구사항]]

## 관련 문서

- [[인증-인가-아키텍처]]
- [[API-클라이언트-가이드]]
- [[화면-정의서]]
