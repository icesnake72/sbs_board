---
title: API 에러코드
tags: [api, error, reference]
updated: 2026-08-01
---

# API 에러코드 레퍼런스

출처: `global/exception/ErrorCode.java`

## 응답 형식

모든 실패 응답은 동일한 형태다.

```json
{
  "code": "POST_NOT_FOUND",
  "message": "게시글을 찾을 수 없습니다.",
  "timestamp": "2026-08-01T14:32:11.123"
}
```

**프론트엔드는 `code`로 분기한다.** `message`는 표시용이며 변경될 수 있다.

---

## 전체 목록

### 404 NOT_FOUND

| code | message | 발생 위치 |
|---|---|---|
| `USER_NOT_FOUND` | 사용자를 찾을 수 없습니다. | 프로필 조회, 게시글/댓글 작성 시 사용자 조회 |
| `BOARD_NOT_FOUND` | 게시판을 찾을 수 없습니다. | 게시판 수정/삭제, 게시글 작성 |
| `POST_NOT_FOUND` | 게시글을 찾을 수 없습니다. | 게시글 조회/수정/삭제, 댓글 작성, 반응 |
| `COMMENT_NOT_FOUND` | 댓글을 찾을 수 없습니다. | 댓글 수정/삭제, 부모 댓글 조회, ⚠️ **알림 조회 시에도 사용됨** |

### 409 CONFLICT

| code | message | 발생 위치 |
|---|---|---|
| `DUPLICATE_USER_EMAIL` | 이미 사용중인 이메일입니다. | 회원가입, 소셜 로그인 이메일 충돌 |
| `DUPLICATE_BOARD_NAME` | 이미 존재하는 게시판입니다. | 게시판 생성 |

### 403 FORBIDDEN

| code | message | 발생 위치 |
|---|---|---|
| `ACCESS_DENIED` | 권한이 없습니다. | `RestAccessDeniedHandler` — `@PreAuthorize` 실패 전반 (소유권·ADMIN) |
| `POST_ACCESS_DENIED` | 게시글을 작성할 권한이 없습니다. | `PostService.validateAuthor()` — 현재 미사용 |
| `BOARD_ACCESS_DENIED` | 게시판을 생성할 권한이 없습니다. | `BoardService.validateUser()` — 현재 미사용 |
| `CANNOT_VIEW_NOTIFICATION` | 해당 알림의 소유자만 조회 가능합니다 | 알림 읽음 처리 |

> 실질적으로 발생하는 403은 대부분 `ACCESS_DENIED`다.

### 401 UNAUTHORIZED

| code | message | 발생 위치 |
|---|---|---|
| `LOGIN_REQUIRED` | 로그인이 필요합니다. | `RestAuthenticationEntryPoint`, **로그인 실패**, 토큰 재발급 실패 |
| `LOGIN_FAILED` | 로그인에 실패하였습니다. | 소셜 로그인 실패 (카카오 토큰/사용자정보 조회) |
| `INVALID_OAUTH_STATE` | 정상적인 인증 요청이 아닙니다. | 레거시 카카오 콜백에서 state 쿠키 없음 |

> ⚠️ 폼 로그인 실패 시 `LOGIN_FAILED`가 아니라 **`LOGIN_REQUIRED`** 가 온다 (`AuthService.login()`의 catch 블록).

### 400 BAD_REQUEST

| code | message | 발생 위치 |
|---|---|---|
| `INVALID_INPUT` | 입력값이 올바르지 않습니다. | `@Valid` 검증 실패 전반 |
| `INVALID_BOARD_ID` | 해당 Board ID는 삭제할 수 없습니다. | **미사용** |
| `INVALID_FILE_TYPE` | 지원하지 않는 파일형식 입니다. | MIME/확장자 불일치, 빈 파일, 경로 탈출 |
| `FILE_COUNT_EXCEEDED` | 파일 업로드 개수 초과 | 이미지 6장 이상 |
| `MAX_UPLOAD_SIZE_EXCEEDED` | 파일 용량 초과 | 2MB/20MB 초과 |
| `CANNOT_REPLY_TO_REPLY` | 대댓글에 댓글을 달 수 없습니다 | 깊이 3 시도 |
| `CANNOT_REPLY_TO_DELETED` | 삭제된 댓글에 댓글을 달 수 없습니다 | |
| `CANNOT_EDIT_DELETED` | 삭제된 댓글은 수정할 수 없습니다 | 삭제된 댓글 수정 시도 |
| `COMMENT_POST_MISMATCH` | 부모 댓글이 해당 게시글의 댓글이 아닙니다 | |
| `SQL_INTEGRITY_ERROR` | 데이터베이스 참조 무결성 위배 에러입니다. | FK 제약 위반 |
| `METHOD_NOT_ALLOWED` | 허용되지 않는 요청 방식입니다. | ⚠️ enum은 400이지만 **응답 상태는 405** |

### 500 INTERNAL_SERVER_ERROR

| code | message | 발생 위치 |
|---|---|---|
| `INVALID_FILE_UPLOAD_DIR` | 파일 업로드 디렉토리 초기화 실패 | 기동 시 `FileStorageService.init()` |
| `FILE_UPLOAD_FAILED` | 파일 업로드 실패 | 디스크 복사 I/O 오류 |
| `INTERNAL_SERVER_ERROR` | 알 수 없는 내부 에러가 발생했습니다. | `NullPointerException` 핸들러 |

---

## 상태코드별 프론트엔드 처리 지침

| 상태 | 처리 |
|---|---|
| **400** | 폼 하단에 `message` 표시. `INVALID_INPUT`은 **어느 필드가 틀렸는지 알 수 없음** → 클라이언트 검증 필수 |
| **401** | Access Token 만료 가능성 → `/api/auth/reissue` 1회 시도 → 실패 시 로그인 페이지로 |
| **403** | "권한이 없습니다" 토스트. 재시도 금지 |
| **404** | 목록으로 이동 또는 "삭제되었거나 없는 글입니다" 표시 |
| **405** | 클라이언트 버그. 개발 중에만 발생해야 함 |
| **409** | 폼 필드에 중복 안내 (이메일/게시판명) |
| **500** | 일반 에러 토스트 + 재시도 버튼 |

---

## ⚠️ 한계

1. **필드 단위 검증 메시지가 없다.** `@Valid` 실패 시 `MethodArgumentNotValidException`의 필드 오류를 버리고 `INVALID_INPUT` 하나만 반환한다.
   ```java
   // GlobalExceptionHandler — 필드 정보를 사용하지 않음
   return ResponseEntity.status(BAD_REQUEST).body(ErrorResponse.of(INVALID_INPUT));
   ```
   → 프론트엔드는 서버와 동일한 규칙을 클라이언트에서 중복 구현해야 한다. [[프론트엔드-요구사항]]

2. 🚨 **핸들러 없는 예외는 401 `LOGIN_REQUIRED`로 위장된다.** (실측 확인)

   `GlobalExceptionHandler`가 잡지 않는 예외 → Spring Boot가 `/error`로 ERROR 디스패치 →
   `SecurityConfig`에 `/error` 허용 규칙이 없어 `anyRequest().authenticated()`에 걸림 →
   `RestAuthenticationEntryPoint`가 401을 반환한다.

   ```
   요청 본문 JSON 파싱 실패 (HttpMessageNotReadableException)
   → 기대: 400   실제: 401 {"code":"LOGIN_REQUIRED","message":"로그인이 필요합니다."}
   ```

   **프론트엔드 영향**: 401을 토큰 만료로 오인해 재발급 → 실패 → **강제 로그아웃**으로 이어질 수 있다.
   `POST /api/auth/*` 이외에서 갑작스러운 401이 뜨면 실제로는 요청 오류일 수 있다.

   서버 수정: `.requestMatchers("/error").permitAll()` + `HttpMessageNotReadableException` 핸들러 추가.
   → [[알려진-이슈]] BE-25

3. `METHOD_NOT_ALLOWED`의 enum 상태(400)와 실제 응답 상태(405)가 불일치.

→ [[알려진-이슈]]

---

## 클라이언트 검증 규칙 (서버와 일치시킬 것)

| 대상 | 규칙 |
|---|---|
| `signup.email` | `@NotBlank` — 서버에 형식 검증 없음. 클라이언트에서 이메일 형식 확인 권장 |
| `signup.password` | 8~30자 |
| `signup.nick_name` | 필수, **스네이크 케이스 키** |
| `login.password` | 8자 이상 |
| `post.title` | **5~200자** |
| `post.body` | 필수 |
| `board.name` | 필수 |
| `comment.content` | 필수, 최대 1000자 |
| 이미지 | 최대 5장 / 각 2MB / png·jpg·jpeg·gif·webp |

## 관련 문서

- [[예외-처리-아키텍처]]
- [[API-클라이언트-가이드]]
- [[API-개요]]
