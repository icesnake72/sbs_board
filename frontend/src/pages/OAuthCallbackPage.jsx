import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { normalizeToken } from '@/lib/api';
import { useAuthStore } from '@/stores/auth';
import { Spinner } from '@/components/ui';

/**
 * 백엔드 OAuth2LoginSuccessHandler 가
 *   302 → /oauth/callback#accessToken=...&id=...&nickName=...   (같은 오리진, 상대 경로)
 * 로 되돌려 보내는 지점이다.
 *
 * fragment(#) 를 쓰는 이유는 쿼리스트링과 달리 서버로 전송되지 않아
 * access log 나 Referer 헤더에 토큰이 남지 않기 때문이다.
 *
 * Refresh Token 은 같은 응답에서 HttpOnly 쿠키로 이미 심어졌으므로 여기서 다룰 것이 없다.
 */
export default function OAuthCallbackPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.slice(1));
    const token = params.get('accessToken');

    if (!token) {
      setFailed(true);
      return;
    }

    // reissue 와 마찬가지로 "Bearer " 접두사가 없으므로 저장 시점에 통일한다.
    const id = params.get('id');
    const nickName = params.get('nickName');
    setAuth(normalizeToken(token), id ? { id: Number(id), nickName } : undefined);

    // replace 로 이동해야 히스토리에서 이 항목(=토큰이 든 fragment)이 사라지고
    // 뒤로가기로 되돌아오지도 않는다.
    navigate('/', { replace: true });
  }, [navigate, setAuth]);

  if (failed) {
    return (
      <div className="flex flex-col items-center gap-3 py-24 text-center">
        <p className="text-slate-600">소셜 로그인에 실패했습니다.</p>
        <button type="button" className="btn-secondary" onClick={() => navigate('/login')}>
          로그인으로
        </button>
      </div>
    );
  }

  return (
    <div className="flex justify-center py-24 text-slate-400">
      <Spinner className="h-6 w-6" />
    </div>
  );
}
