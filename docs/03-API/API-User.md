---
title: API User
tags: [api, user, profile]
updated: 2026-08-01
---

# API: 사용자 (`/api/user`)

`user/UserProfileController.java`

---

## GET `/api/user/me` — 내 프로필

권한: 🔒 인증 필요 (`SecurityConfig`에 명시적으로 `.authenticated()`)

### 의도한 응답 `200 OK`

```json
{
  "nickName": "홍길동",
  "phoneNumber": null,
  "birth": "",
  "createdAt": "2026-07-21T10:00:00"
}
```

`UserProfileResponse` — `birth`는 null일 때 **빈 문자열 `""`** 로 치환된다.

`email`, `role`, `profileImageUrl`은 **포함되지 않는다**.

### ⚪ 현재 미동작 — 의도된 상태 (추후 구현 예정)

> **결정(2026-08-01)**: `WebConfig`의 리졸버 주석 처리는 의도된 것이며 프로필 기능은 추후 구현한다.
> 아래는 구현 시 참고할 내용.

컨트롤러 시그니처:

```java
@GetMapping("/me")
public UserProfileResponse me(@LoginUserId Long loginUserId) { ... }
```

`@LoginUserId`를 처리하는 `LoginUserIdResolver`가 **`WebConfig`에 등록되어 있지 않다** (해당 코드가 주석 처리됨).

```java
// WebConfig.java
//    @Override
//    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
//        resolvers.add(loginUserIdResolver);
//    }
```

결과:
- Spring이 `Long loginUserId`를 **일반 쿼리 파라미터**로 취급 → 값이 없으면 `null`
- `UserProfileService.me(null)` → `userRepository.findById(null)` 에서 예외 → **500** 또는 `LOGIN_REQUIRED`

게다가 리졸버가 참조하는 `request.getAttribute("LoginUserId")`는 현재 어떤 필터도 설정하지 않는다
(`JwtAuthenticationFilter`는 `SecurityContext`만 채운다). 리졸버를 다시 켜도 동작하지 않는다.

→ [[알려진-이슈]]

### 구현 시 권장 방법

다른 컨트롤러들과 동일하게 `@AuthenticationPrincipal`로 통일한다.

```java
@GetMapping("/me")
public UserProfileResponse me(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return userProfileService.me(userDetails.getId());
}
```

`@LoginUserId` / `LoginUserIdResolver`는 세션·필터 기반 시절의 잔재이므로 삭제 후보다.

### 에러 (수정 후 기준)

| 상태 | code | 조건 |
|---|---|---|
| 401 | `LOGIN_REQUIRED` | 미인증 / 사용자 없음 |
| 404 | `USER_NOT_FOUND` | 프로필 레코드 없음 |

---

## 미구현 API

프론트엔드 마이페이지를 만들려면 아래가 필요하다.

| 필요한 API | 용도 |
|---|---|
| `PUT /api/user/me` | `nickName`, `phoneNumber`, `birth` 수정 |
| `GET /api/user/me/posts` | 내가 쓴 글 |
| `GET /api/user/me/comments` | 내가 쓴 댓글 |
| `PUT /api/user/me/password` | 비밀번호 변경 |
| `GET /api/user/{id}` | 다른 사용자 공개 프로필 |
| `DELETE /api/user/me` | 회원 탈퇴 |

현재 `UserProfile.phoneNumber`와 `birth`를 **채울 방법이 전혀 없다** (가입 시 null로 생성되고 수정 API가 없음).

→ [[프론트엔드-요구사항]] FR-2

---

## 로그인 사용자 정보를 얻는 대안

`/api/user/me`가 고쳐지기 전까지는 **로그인 응답을 클라이언트에 보관**하는 수밖에 없다.

```json
// POST /api/auth/login 응답
{ "id": 1, "email": "...", "nickName": "홍길동", "accessToken": "Bearer ...", ... }
```

단점: 새로고침 시 `id`/`nickName`을 복원하려면 별도 저장(localStorage 등)이 필요하고,
Access Token만으로는 사용자 정보를 다시 가져올 수 없다.
→ [[토큰-저장-전략]]

## 관련 문서

- [[엔티티-User]] · [[엔티티-UserProfile]]
- [[인증-인가-아키텍처]]
- [[알려진-이슈]]
