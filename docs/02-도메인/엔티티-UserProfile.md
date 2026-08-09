---
title: 엔티티 UserProfile
tags: [domain, entity, user]
updated: 2026-08-01
---

# 엔티티: UserProfile

`global/entity/UserProfile.java` → 테이블 `user_profiles`

인증 정보(`User`)와 부가 프로필 정보를 분리한 테이블.

## 필드

| 필드 | 컬럼 | 타입 | 제약 |
|---|---|---|---|
| `id` | `id` | Long | PK |
| `user` | `user_id` | `@OneToOne(LAZY)` | **NOT NULL, UNIQUE** |
| `phoneNumber` | `phone_number` | String(30) | nullable |
| `birth` | `birth` | LocalDate | nullable |
| `createdAt` | `created_at` | LocalDateTime | 기본 `now()` |
| `updatedAt` | `updated_at` | LocalDateTime | `@LastModifiedDate` (`@EntityListeners` ✅) |

## 생성 시점

프로필은 **항상 User와 함께 자동 생성**된다. 별도 가입 절차가 없다.

| 경로 | 코드 |
|---|---|
| 로컬 회원가입 | `AuthService.signUp()` — `existsByUser` 확인 후 빈 프로필 저장 |
| 소셜 로그인 최초 | `CustomOAuth2UserService.findOrCreate()` — `findByUser().orElseGet(...)` |

생성 시 `phoneNumber`, `birth`는 **null**이다.

## 리포지토리

`user/UserProfileRepository.java`

```java
boolean existsByUser(User user);
Optional<UserProfile> findByUser(User user);
```

## DTO 변환

```java
UserProfile.toDTO(profile) → UserProfileResponse {
    nickName,      // profile.getUser().getNickName()  ← User에서 가져옴
    phoneNumber,
    birth,         // null이면 빈 문자열 ""
    createdAt      // toString()
}
```

`birth`가 null일 때 `""`로 치환하는 점에 주의 (프론트엔드에서 빈 문자열 처리 필요).

## 미구현

- **프로필 수정 API가 없다.** `phoneNumber`, `birth`를 채울 방법이 현재 없음
- 프로필 이미지는 `User.profileImageUrl`에 있고 `UserProfileResponse`에 포함되지 않음
- 조회 API(`GET /api/user/me`)가 현재 동작하지 않음 → [[알려진-이슈]]

프론트엔드 요구사항은 [[프론트엔드-요구사항]] FR-2 참조.

## 관련 문서

- [[엔티티-User]]
- [[API-User]]
