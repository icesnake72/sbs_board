---
title: 엔티티 User
tags: [domain, entity, user, auth]
updated: 2026-08-01
---

# 엔티티: User

`global/entity/User.java` → 테이블 `users`

로컬 가입 사용자와 소셜 로그인 사용자를 **한 테이블**에서 관리한다.

## 필드

| 필드 | 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| `id` | `id` | Long | PK, IDENTITY | |
| `email` | `email` | String(500) | **NOT NULL, UNIQUE** | 로그인 ID. JWT `subject`로도 쓰임 |
| `password` | `password` | String(200) | NOT NULL | BCrypt 해시. 소셜 계정은 랜덤 UUID 해시 |
| `nickName` | `nick_name` | String(100) | NOT NULL | 화면 표시명 |
| `provider` | `provider` | String(100) | nullable | `KAKAO` / `GOOGLE` / null(로컬) |
| `providerId` | `provider_id` | String(100) | nullable | `"{PROVIDER}_{고유ID}"` 형식 |
| `profileImageUrl` | `profile_image_url` | String(500) | nullable | 소셜 프로필 이미지 URL |
| `role` | `role` | enum(STRING) | 기본 `USER` | `ADMIN` \| `USER` |
| `createdAt` | `created_at` | LocalDateTime | 기본 `now()` | |
| `updatedAt` | `updated_at` | LocalDateTime | `@LastModifiedDate` | `@EntityListeners` 있음 ✅ |

```java
public enum Role { ADMIN, USER }
```

## 계정 유형 구분

| 유형 | `provider` | `providerId` | `password` | 폼 로그인 |
|---|---|---|---|---|
| 로컬 가입 | null | null | 사용자 비밀번호 해시 | ✅ 가능 |
| 카카오 | `KAKAO` | `KAKAO_123456` | 랜덤 UUID 해시 | ❌ 불가 |
| 구글 | `GOOGLE` | `GOOGLE_{sub}` | 랜덤 UUID 해시 | ❌ 불가 |

> 소셜 사용자는 비밀번호를 알 수 없으므로 `POST /api/auth/login`으로는 로그인할 수 없다.
> 반드시 해당 소셜 경로를 써야 한다. [[API-OAuth]]

## 이메일 중복 정책

- 로컬 가입: `existsByEmail` 검사 → `DUPLICATE_USER_EMAIL` (409)
- 소셜 가입: `CustomOAuth2UserService.verifySocialEmail()`에서 동일 email이 이미 있으면 `OAuth2DuplicateEmailException` → 409
- **계정 연동(linking)은 미구현.** 같은 이메일로 로컬 가입 후 구글 로그인 시도 → 실패한다.

## 리포지토리

`auth/UserRepository.java`

```java
boolean existsByEmail(String email);
Optional<User> findByEmail(String email);      // 로그인 / JWT 검증
Optional<User> findByProviderId(String id);    // 소셜 로그인
```

## 정적 팩토리

```java
User.from(email, nickName, password, role)   // 정의돼 있으나 실제로는 setter 방식이 주로 쓰임
```

## 연관 관계

- `UserProfile` 1:1 — [[엔티티-UserProfile]] (User 쪽에는 참조 필드가 없다. **단방향**)
- `RefreshToken` 1:1 — [[엔티티-RefreshToken]] (FK가 아니라 `Long userId` 단순 컬럼)
- `Post` 1:N — [[엔티티-Post]] (`posts.user_id`)
- `Comment` 1:N — [[엔티티-Comment]]
- `Notification` — recipient / actor 두 방향 — [[엔티티-Notification]]

## 보안 관련

- `CustomUserDetails`가 이 엔티티를 감싼다 → `ROLE_` 접두사를 붙여 권한 부여
- `ADMIN` 역할은 [[API-Board]]의 게시판 생성/수정/삭제에 필요
- 회원가입 시 `role`을 **클라이언트가 문자열로 지정**한다 (`SignupRequest.role`) → **누구나 ADMIN 가입 가능**. 심각한 이슈. [[알려진-이슈]] 참조

## 응답 노출 형태

| DTO | 노출 필드 | 사용처 |
|---|---|---|
| `UserResponse` | `id`, `email`, `nickName`, `accessToken`, `refreshToken`, `role` | 로그인/소셜 로그인 응답 |
| `UserProfileResponse` | `nickName`, `phoneNumber`, `birth`, `createdAt` | `GET /api/user/me` |
| 게시글/댓글 내 작성자 | `nickName` 문자열만 | `PostDTO.author`, `CommentResponse.authorUsername` |

> `password`는 어떤 DTO에도 포함되지 않는다.
> `UserResponse.refreshToken`은 디버그용이며 운영에서는 제거해야 한다 (코드 주석에 명시됨).

## 관련 문서

- [[인증-인가-아키텍처]]
- [[API-Auth]] · [[API-OAuth]] · [[API-User]]
