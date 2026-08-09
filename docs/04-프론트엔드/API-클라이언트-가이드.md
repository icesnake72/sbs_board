---
title: API 클라이언트 가이드
tags: [frontend, http, client]
updated: 2026-08-01
---

# API 클라이언트 구현 가이드

이 백엔드의 **특이사항을 흡수하는** fetch 래퍼 설계.

## 흡수해야 할 특이사항

| # | 특이사항 | 대응 |
|---|---|---|
| 1 | `login`은 `"Bearer "` 포함, `reissue`는 미포함 | 저장 시 정규화 |
| 2 | 일부 응답이 `text/plain` (`"ok"`) | Content-Type 확인 후 파싱 |
| 3 | 일부 응답이 **바디 없음** (204 아닌 200) | 빈 바디 안전 처리 |
| 4 | 에러 바디에 `code`가 없을 수 있음 (핸들러 미등록 예외) | 폴백 |
| 5 | Refresh 쿠키 필요 | `credentials: 'include'` |
| 6 | 401 시 자동 재발급 | 인터셉터 + 중복 방지 |
| 7 | 게시글 작성만 multipart | 별도 경로 |

---

## 1. 개발 환경 프록시 (권장)

CORS 미설정 문제를 우회하는 가장 빠른 방법이다. → [[알려진-이슈]] BE-01

### Vite

```ts
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api':    { target: 'http://localhost:8090', changeOrigin: true },
      '/images': { target: 'http://localhost:8090', changeOrigin: true },
      '/oauth2': { target: 'http://localhost:8090', changeOrigin: true },
      '/login/oauth2': { target: 'http://localhost:8090', changeOrigin: true },
    },
  },
});
```

### Next.js

```js
// next.config.js
module.exports = {
  async rewrites() {
    return [
      { source: '/api/:path*',    destination: 'http://localhost:8090/api/:path*' },
      { source: '/images/:path*', destination: 'http://localhost:8090/images/:path*' },
      { source: '/oauth2/:path*', destination: 'http://localhost:8090/oauth2/:path*' },
    ];
  },
};
```

프록시를 쓰면 `API_BASE`가 빈 문자열이 되고, 쿠키도 동일 오리진으로 취급된다.

```ts
export const API_BASE = import.meta.env.VITE_API_BASE ?? '';
```

---

## 2. fetch 래퍼

```ts
// src/lib/api.ts
import { useAuthStore } from '@/stores/auth';
import type { ErrorResponse, ErrorCode } from '@/types/api';

export const API_BASE = import.meta.env.VITE_API_BASE ?? '';

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: ErrorCode | 'UNKNOWN',
    message: string,
  ) {
    super(message);
  }
}

export const normalizeToken = (t: string) =>
  t.startsWith('Bearer ') ? t : `Bearer ${t}`;

/** 응답 바디 파싱 — JSON / text / 빈 바디 모두 처리 */
async function parseBody(res: Response): Promise<unknown> {
  const raw = await res.text();
  if (!raw) return null;                       // 특이사항 3
  const ct = res.headers.get('content-type') ?? '';
  if (ct.includes('application/json')) {
    try { return JSON.parse(raw); } catch { return raw; }
  }
  return raw;                                  // 특이사항 2: "ok"
}

// ── 401 재발급: 동시 요청 중복 방지 ──────────────
let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (refreshing) return refreshing;           // 진행 중인 것 재사용

  refreshing = (async () => {
    try {
      const res = await fetch(`${API_BASE}/api/auth/reissue`, {
        method: 'POST',
        credentials: 'include',                // Refresh 쿠키 전송
      });
      if (!res.ok) return null;
      const data = (await res.json()) as { accessToken: string };
      const token = normalizeToken(data.accessToken);   // 특이사항 1
      useAuthStore.getState().setAccessToken(token);
      return token;
    } catch {
      return null;
    } finally {
      refreshing = null;
    }
  })();

  return refreshing;
}

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  skipAuth?: boolean;
  _retried?: boolean;
}

export async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const { body, skipAuth, _retried, headers, ...rest } = opts;
  const token = useAuthStore.getState().accessToken;

  const isFormData = body instanceof FormData;
  const h = new Headers(headers);

  if (!skipAuth && token) h.set('Authorization', token);
  // FormData일 때 Content-Type을 설정하면 boundary가 깨진다 (특이사항 7)
  if (body !== undefined && !isFormData) h.set('Content-Type', 'application/json');

  const res = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: h,
    credentials: 'include',                    // 특이사항 5
    body: isFormData ? body : body !== undefined ? JSON.stringify(body) : undefined,
  });

  // ── 401 → 재발급 후 1회 재시도 (특이사항 6) ──
  // ⚠️ 서버가 미처리 예외도 401 LOGIN_REQUIRED로 반환한다([[알려진-이슈]] BE-25).
  //    재발급이 성공했는데 재시도도 401이면 토큰 문제가 아니므로 로그아웃시키지 않는다.
  if (res.status === 401 && !_retried && !skipAuth && !path.startsWith('/api/auth/')) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      return request<T>(path, { ...opts, _retried: true });
    }
    useAuthStore.getState().clear();
    if (typeof window !== 'undefined') window.location.href = '/login';
  }

  const data = await parseBody(res);

  if (!res.ok) {
    const e = data as Partial<ErrorResponse> | null;
    throw new ApiError(                        // 특이사항 4
      res.status,
      e?.code ?? 'UNKNOWN',
      e?.message ?? `요청에 실패했습니다 (${res.status})`,
    );
  }

  return data as T;
}

export const api = {
  get:    <T>(p: string, o?: RequestOptions) => request<T>(p, { ...o, method: 'GET' }),
  post:   <T>(p: string, body?: unknown, o?: RequestOptions) => request<T>(p, { ...o, method: 'POST', body }),
  put:    <T>(p: string, body?: unknown, o?: RequestOptions) => request<T>(p, { ...o, method: 'PUT', body }),
  delete: <T>(p: string, o?: RequestOptions) => request<T>(p, { ...o, method: 'DELETE' }),
};
```

---

## 3. 도메인별 호출 함수

```ts
// src/api/auth.ts
import { api, normalizeToken } from '@/lib/api';
import type { UserResponse, IngestResult, SignupRequest, LoginRequest } from '@/types/api';

export const signup = (v: { email: string; password: string; nickName: string }) =>
  api.post<IngestResult>('/api/auth/signup', {
    email: v.email,
    password: v.password,
    nick_name: v.nickName,   // 스네이크 케이스 변환
    role: 'USER',            // 항상 고정
  } satisfies SignupRequest, { skipAuth: true });

export const login = async (v: LoginRequest) => {
  const res = await api.post<UserResponse>('/api/auth/login', v, { skipAuth: true });
  return { ...res, accessToken: normalizeToken(res.accessToken) };
};

export const logout = () => api.post<void>('/api/auth/logout');
```

```ts
// src/api/post.ts
import { api, API_BASE } from '@/lib/api';
import type { Page, PostDTO, PostRequest } from '@/types/api';

export const getPosts = (boardId: number, page = 0, size = 10, sort = 'createdAt,DESC') =>
  api.get<Page<PostDTO>>(
    `/api/post/${boardId}/all?${new URLSearchParams({ page: String(page), size: String(size), sort })}`,
  );

export const getPost = (id: number) => api.get<PostDTO>(`/api/post/${id}`);

/** 게시글 작성 — multipart */
export const createPost = (boardId: number, post: PostRequest, images: File[]) => {
  const fd = new FormData();
  fd.append('post', new Blob([JSON.stringify(post)], { type: 'application/json' }));
  images.forEach(f => fd.append('images', f));
  return api.post<PostDTO>(`/api/post/${boardId}/new`, fd);
};

export const updatePost = (id: number, post: PostRequest) =>
  api.put<PostDTO>(`/api/post/${id}/update`, post);

/** ⚠️ 응답이 text/plain "ok" */
export const deletePost = (id: number) => api.delete<string>(`/api/post/${id}`);

/** 이미지 절대 경로 */
export const imageUrl = (url: string) => `${API_BASE}${url}`;
```

```ts
// src/api/comment.ts
export const getComments = (postId: number, page = 0, size = 10, sort = 'createdAt,ASC') =>
  api.get<Page<CommentResponse>>(
    `/api/comment/post/${postId}/list?${new URLSearchParams({ page: String(page), size: String(size), sort })}`,
  );

export const createComment = (postId: number, content: string, parentId?: number) =>
  api.post<CommentResponse>(`/api/comment/post/${postId}/new`, { content, parentId });

export const deleteComment = (id: number) => api.delete<void>(`/api/comment/${id}`);
```

```ts
// src/api/reaction.ts
// ⚠️ 게시글은 서버 경로가 "reation"(오타), 댓글은 "reaction"(정상) — 철자가 다르다
export const reactToPost = (postId: number, type: ReactionType) =>
  api.post<ReactionResponse>(`/api/post/${postId}/reation`, { type });

export const reactToComment = (commentId: number, type: ReactionType) =>
  api.post<ReactionResponse>(`/api/comment/${commentId}/reaction`, { type });
```

```ts
// src/api/notification.ts
export const getUnreadCount = () => api.get<{ count: number }>('/api/notify/unreads');
export const getNotifications = (page = 0, size = 10) =>
  api.get<Page<NotificationResponse>>(`/api/notify/list?page=${page}&size=${size}`);
export const readNotification = (id: number) => api.put<void>(`/api/notify/${id}/read`);
```

---

## 4. TanStack Query 연동

```ts
// 게시글 목록
export const usePosts = (boardId: number, page: number) =>
  useQuery({
    queryKey: ['posts', boardId, page],
    queryFn: () => getPosts(boardId, page),
    placeholderData: keepPreviousData,   // 페이지 전환 시 깜빡임 방지
  });

// 댓글 작성 후 목록 무효화
export const useCreateComment = (postId: number) => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { content: string; parentId?: number }) =>
      createComment(postId, v.content, v.parentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['comments', postId] });
      qc.invalidateQueries({ queryKey: ['notify', 'unread'] });
    },
  });
};

// 알림 뱃지 폴링
export const useUnreadCount = () =>
  useQuery({
    queryKey: ['notify', 'unread'],
    queryFn: getUnreadCount,
    refetchInterval: 30_000,
    enabled: useAuthStore(s => !!s.accessToken),
  });
```

---

## 5. 에러 처리

```ts
// src/lib/errorMessage.ts
import { ApiError } from './api';

const OVERRIDE: Partial<Record<string, string>> = {
  LOGIN_REQUIRED:       '이메일 또는 비밀번호가 올바르지 않습니다',
  DUPLICATE_USER_EMAIL: '이미 사용 중인 이메일입니다',
  CANNOT_REPLY_TO_REPLY:'대댓글에는 답글을 달 수 없습니다',
  CANNOT_EDIT_DELETED:  '현재 댓글 수정이 불가합니다',   // 서버 버그 우회 안내
  UNKNOWN:              '알 수 없는 오류가 발생했습니다',
};

export const toMessage = (e: unknown) =>
  e instanceof ApiError ? (OVERRIDE[e.code] ?? e.message) : '네트워크 오류가 발생했습니다';
```

`INVALID_INPUT`은 어느 필드가 틀렸는지 알 수 없다 → **클라이언트 검증이 1차 방어선**이다.
→ [[프론트엔드-데이터-모델]]의 Zod 스키마 참조.

---

## 6. 부팅 시 세션 복원

```ts
// App 최상단에서 1회
useEffect(() => {
  (async () => {
    const res = await fetch(`${API_BASE}/api/auth/reissue`, {
      method: 'POST',
      credentials: 'include',
    });
    if (res.ok) {
      const { accessToken } = await res.json();
      useAuthStore.getState().setAccessToken(normalizeToken(accessToken));
      // 🚧 GET /api/user/me 복구 후 사용자 정보도 함께 로드
    }
    useAuthStore.getState().setInitialized(true);
  })();
}, []);
```

`initialized`가 false인 동안에는 인증 의존 UI를 렌더링하지 않는다 (깜빡임 방지).

→ [[토큰-저장-전략]]

---

## 체크리스트

- [ ] 개발 프록시 설정 (또는 백엔드 CORS 추가)
- [ ] `credentials: 'include'` 전역 적용
- [ ] 토큰 정규화 (`normalizeToken`)
- [ ] 401 재발급 + 중복 방지 + 1회 재시도 제한
- [ ] `text/plain` 및 빈 바디 응답 처리
- [ ] `code` 없는 에러 폴백
- [ ] multipart 요청에서 `Content-Type` 미설정
- [ ] Zod 클라이언트 검증

## 관련 문서

- [[API-개요]] · [[API-에러코드]] · [[API-페이지네이션]]
- [[프론트엔드-데이터-모델]]
- [[토큰-저장-전략]]
