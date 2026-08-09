import { create } from 'zustand';

/**
 * Access Token 은 메모리에만 둔다 (XSS 대비). 새로고침 시에는 Refresh 쿠키로 재발급한다.
 *
 * reissue 응답에는 id/nickName 이 없고 GET /api/user/me 도 아직 없으므로,
 * 민감하지 않은 표시용 정보(id, nickName)만 localStorage 에 보관한다 — 토큰은 절대 넣지 않는다.
 * → docs/04-프론트엔드/토큰-저장-전략.md 의 대응 B
 */
const USER_KEY = 'board.user';

function loadUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function saveUser(user) {
  try {
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
    else localStorage.removeItem(USER_KEY);
  } catch {
    /* 저장 실패는 무시 — 표시용 정보일 뿐이다 */
  }
}

export const useAuthStore = create((set) => ({
  accessToken: null,
  user: loadUser(), // { id, nickName } | null
  initialized: false,

  setAccessToken: (accessToken) => set({ accessToken }),

  setAuth: (accessToken, user) => {
    if (user !== undefined) saveUser(user);
    set(user !== undefined ? { accessToken, user } : { accessToken });
  },

  setInitialized: (initialized) => set({ initialized }),

  clear: () => {
    saveUser(null);
    set({ accessToken: null, user: null });
  },
}));

export const useIsLoggedIn = () => useAuthStore((s) => !!s.accessToken);
