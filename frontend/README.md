# board-frontend

`docs/` 위키(특히 `04-프론트엔드/`)를 기준으로 만든 React + JavaScript 프론트엔드.

## 실행

```bash
# 1) 백엔드 먼저 (프로젝트 루트에서)
./gradlew bootRun          # http://localhost:8090

# 2) 프론트엔드
cd frontend
npm install
npm run dev                # http://localhost:3000
```

`npm run build` → `dist/`, `npm run preview` 로 빌드 결과 확인.

## 기술 스택

| 영역 | 선택 |
|---|---|
| 빌드 | Vite 6 |
| 프레임워크 | React 18 (JavaScript / JSX) |
| 라우팅 | React Router 6 |
| 서버 상태 | TanStack Query 5 |
| 클라이언트 상태 | Zustand (인증·토스트) |
| 폼 | React Hook Form + Zod |
| 스타일 | Tailwind CSS 3 |

> 위키는 TypeScript를 권장하지만, 요청에 따라 JavaScript로 구성했다.
> 타입 정의는 `docs/04-프론트엔드/프론트엔드-데이터-모델.md` 를 참조하고,
> 응답 형태는 각 `src/api/*.js` 주석에 남겨 두었다.

## 구조

```
src/
  lib/
    api.js            fetch 래퍼 (401 재발급 · 토큰 정규화 · text/plain · 빈 바디)
    schemas.js        Zod 검증 스키마 (서버 규칙과 동일하게 유지할 것)
    errorMessage.js   ErrorCode → 사용자 문구
    constants.js      Page 헬퍼, 이미지 제한값
    format.js         날짜/숫자 포맷
  stores/             auth(메모리 토큰), toast
  api/                도메인별 호출 함수
  hooks/queries.js    TanStack Query 훅 모음
  components/         Layout, 댓글 트리, 이미지 업로더/갤러리, 반응 버튼 등
  pages/              화면 단위 컴포넌트
```

## 라우트 ↔ 화면 정의서

| 라우트 | 화면 | 비고 |
|---|---|---|
| `/` | SCR-01 메인 | 게시판 카드 + 게시판별 최근 5건 |
| `/boards/:boardId` | SCR-02 게시글 목록 | 페이징(0-based), 최신순/조회순 |
| `/posts/:postId` | SCR-03 상세 | 이미지 갤러리 · 반응 · 댓글 |
| `/boards/:boardId/posts/new` | SCR-04 작성 🔒 | multipart, 이미지 ≤5 |
| `/posts/:postId/edit` | SCR-05 수정 ✍️ | 제목·본문만 |
| `/login` | SCR-06 | 폼 + 카카오/구글 |
| `/signup` | SCR-07 | |
| `/oauth/callback` | SCR-08 🚧 | 백엔드 수정 필요 (아래 참조) |
| (헤더 벨) | SCR-09 알림 | 30초 폴링 |
| `/admin/boards` | SCR-11 👑🚧 | 메뉴 미노출, 직접 URL 접근 |

SCR-10 마이페이지(`/me`)는 위키에서 "추후 구현"으로 정리되어 만들지 않았다.

## 백엔드 특이사항 대응

`docs/04-프론트엔드/API-클라이언트-가이드.md` 의 항목을 그대로 흡수했다.

| 특이사항 | 대응 위치 |
|---|---|
| `login`은 `"Bearer "` 포함, `reissue`는 미포함 | `lib/api.js` `normalizeToken` |
| 일부 응답이 `text/plain` (`"ok"`) | `lib/api.js` `parseBody` |
| 일부 응답이 빈 바디 | 같은 곳 |
| 에러 바디에 `code`가 없을 수 있음 | `ApiError` 폴백 `'UNKNOWN'` |
| Refresh 쿠키 | 전 요청 `credentials: 'include'` |
| 401 자동 재발급 | 진행 중 Promise 공유 + 1회 재시도 제한 |
| 게시글 작성만 multipart | `api/post.js`, `Content-Type` 미설정 |
| 게시글 반응 경로 오타 `reation` | `api/reaction.js` |
| `page`는 `{content, page:{...}}` (VIA_DTO) | `lib/constants.js` |
| 없는 `boardId`도 빈 페이지 | 게시판 존재 확인은 `/api/board/all` 로 |

### CORS / 쿠키

백엔드 CORS가 아직 없고(BE-01) Refresh 쿠키가 `SameSite=Strict` 라서,
`vite.config.js` 의 프록시로 `/api`·`/images`·`/oauth2` 를 `:8090` 으로 넘겨 **동일 오리진**처럼 동작시킨다.
따라서 `VITE_API_BASE` 는 비워 두는 것이 기본값이다.

백엔드에 CORS가 적용되면 `.env` 에 `VITE_API_BASE=http://localhost:8090` 을 넣어 직접 호출로 전환할 수 있다.

## 백엔드 수정이 필요한 부분 (현재 제약)

| # | 내용 | 프론트 현재 동작 |
|---|---|---|
| P-1/P-2 | CORS·쿠키 설정 | 개발 프록시로 우회 |
| FR-1.3 | `OAuth2LoginSuccessHandler` 가 JSON을 그대로 출력 | `/oauth/callback` 화면은 만들어 두었으나 도달 불가. 아래처럼 리다이렉트하도록 수정 필요 |
| FR-2 | `GET /api/user/me` 없음 | 새로고침 시 닉네임 복원 불가 → `id`/`nickName` 만 `localStorage` 보관 (토큰은 메모리) |
| FR-3.2 | 로그인 응답 `role` 이 항상 null | ADMIN 판별 불가 → `/admin/boards` 를 메뉴에 노출하지 않음 |
| FR-4.4 | 이미지 수정 API 없음 | 수정 화면에서 이미지 읽기 전용 + 안내 문구 |
| FR-5.x | `CommentResponse` 에 작성자 ID 없음 | 닉네임 문자열 비교로 내 댓글 판별 (중복 닉네임에서 오작동) |
| BE-25 | `/error` 가 permitAll 아님 → 미처리 예외가 401로 위장 | 재시도 1회 제한, 재발급 실패 시에만 로그아웃 |

소셜 로그인 수정 예:

```java
String redirect = UriComponentsBuilder
    .fromUriString(frontendUrl + "/oauth/callback")
    .fragment("accessToken=" + accessToken)
    .build().toUriString();
response.sendRedirect(redirect);
```

## 토큰 저장

- **Access Token**: 메모리(Zustand)에만 보관 — XSS로 유출되지 않는다.
- **새로고침**: 부팅 시 `POST /api/auth/reissue` 1회로 세션 복원 (`App.jsx`).
- **표시용 `id`/`nickName`**: `localStorage` (민감정보 아님). **토큰은 절대 넣지 않는다.**
