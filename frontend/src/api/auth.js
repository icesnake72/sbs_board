import { api, normalizeToken } from '@/lib/api';

/** ⚠️ 키가 nick_name(스네이크), role 은 항상 "USER" 고정 전송 */
export const signup = (v) =>
  api.post(
    '/api/auth/signup',
    { email: v.email, password: v.password, nick_name: v.nickName, role: 'USER' },
    { skipAuth: true },
  );

/** 응답 accessToken 에는 이미 "Bearer " 가 붙어 있지만, 안전하게 정규화한다. */
export const login = async (v) => {
  const res = await api.post('/api/auth/login', v, { skipAuth: true });
  return { ...res, accessToken: normalizeToken(res.accessToken) };
};

/** 쿠키가 없어도 200 이므로 실패 처리는 불필요하다. */
export const logout = () => api.post('/api/auth/logout');
