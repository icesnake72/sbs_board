import { useAuthStore } from '@/stores/auth';

/**
 * 프록시를 쓰면 빈 문자열이 되고, 쿠키도 동일 오리진으로 취급된다.
 * → docs/04-프론트엔드/API-클라이언트-가이드.md
 */
export const API_BASE = import.meta.env.VITE_API_BASE ?? '';

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

/**
 * 특이사항 1: login 응답은 "Bearer " 를 포함하고, reissue 응답은 포함하지 않는다.
 * 저장 시점에 항상 "Bearer xxx" 형태로 통일한다.
 */
export const normalizeToken = (t) => (t.startsWith('Bearer ') ? t : `Bearer ${t}`);

/** 이미지 절대 경로 조립 — PostImageResponse.url 은 "/images/{storedName}" 형태다. */
export const imageUrl = (url) => `${API_BASE}${url}`;

/**
 * 응답 바디 파싱 — JSON / text/plain("ok") / 빈 바디를 모두 처리한다.
 * 특이사항 2, 3.
 */
async function parseBody(res) {
  const raw = await res.text();
  if (!raw) return null;
  const ct = res.headers.get('content-type') ?? '';
  if (ct.includes('application/json')) {
    try {
      return JSON.parse(raw);
    } catch {
      return raw;
    }
  }
  return raw;
}

// ── 401 재발급: 동시 요청이 몰려도 reissue 는 1회만 ──────────────
let refreshing = null;

async function refreshAccessToken() {
  if (refreshing) return refreshing;

  refreshing = (async () => {
    try {
      const res = await fetch(`${API_BASE}/api/auth/reissue`, {
        method: 'POST',
        credentials: 'include', // Refresh 쿠키 전송
      });
      if (!res.ok) return null;
      const data = await res.json();
      const token = normalizeToken(data.accessToken);
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

/** 부팅 시 세션 복원용 — 실패해도 조용히 null 을 돌려준다. */
export const restoreSession = () => refreshAccessToken();

export async function request(path, opts = {}) {
  const { body, skipAuth, _retried, headers, ...rest } = opts;
  const token = useAuthStore.getState().accessToken;

  const isFormData = body instanceof FormData;
  const h = new Headers(headers);

  if (!skipAuth && token) h.set('Authorization', token);
  // FormData 에 Content-Type 을 지정하면 boundary 가 깨진다 (특이사항 7).
  if (body !== undefined && !isFormData) h.set('Content-Type', 'application/json');

  const res = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: h,
    credentials: 'include', // 특이사항 5
    body: isFormData ? body : body !== undefined ? JSON.stringify(body) : undefined,
  });

  // ── 401 → 재발급 후 1회 재시도 (특이사항 6) ──
  // ⚠️ 서버는 핸들러 없는 예외도 401 LOGIN_REQUIRED 로 반환한다(알려진-이슈 BE-25).
  //    그래서 재시도는 1회로 제한하고, 재발급 자체가 실패한 경우에만 로그아웃시킨다.
  if (res.status === 401 && !_retried && !skipAuth && !path.startsWith('/api/auth/')) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      return request(path, { ...opts, _retried: true });
    }
    useAuthStore.getState().clear();
    if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
      window.location.href = '/login';
    }
  }

  const data = await parseBody(res);

  if (!res.ok) {
    const e = data && typeof data === 'object' ? data : null;
    throw new ApiError(
      res.status,
      e?.code ?? 'UNKNOWN', // 특이사항 4: code 가 없을 수 있다
      e?.message ?? `요청에 실패했습니다 (${res.status})`,
    );
  }

  return data;
}

export const api = {
  get: (p, o) => request(p, { ...o, method: 'GET' }),
  post: (p, body, o) => request(p, { ...o, method: 'POST', body }),
  put: (p, body, o) => request(p, { ...o, method: 'PUT', body }),
  delete: (p, o) => request(p, { ...o, method: 'DELETE' }),
};
