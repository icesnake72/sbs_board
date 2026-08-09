import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { loginSchema } from '@/lib/schemas';
import { login as loginApi } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';
import { messageForCode, toMessage } from '@/lib/errorMessage';
import { API_BASE } from '@/lib/api';
import { Spinner } from '@/components/ui';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const setAuth = useAuthStore((s) => s.setAuth);
  const from = location.state?.from ?? '/';

  // 소셜 로그인 실패 시 백엔드가 /login?error=CODE 로 되돌려 보낸다.
  const [searchParams] = useSearchParams();
  const socialError = searchParams.get('error');

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } });

  const onSubmit = async (values) => {
    try {
      const res = await loginApi(values);
      // role 은 항상 null 로 내려오므로 저장하지 않는다.
      setAuth(res.accessToken, { id: res.id, nickName: res.nickName });
      navigate(from, { replace: true });
    } catch (e) {
      setError('root', { message: toMessage(e) });
    }
  };

  /**
   * 소셜 로그인은 fetch 로 불가능하다 — 전체 페이지 이동이 필요하다.
   * 이후 흐름: provider 동의화면 → 백엔드 /login/oauth2/code/{provider}
   *          → OAuth2LoginSuccessHandler 가 /oauth/callback#accessToken=... 으로 302
   *          → OAuthCallbackPage 가 토큰을 저장하고 메인('/')으로 replace 이동
   */
  const social = (provider) => {
    window.location.href = `${API_BASE}/oauth2/authorization/${provider}`;
  };

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-6 text-center text-2xl font-bold">로그인</h1>

      {socialError && (
        <p className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
          {messageForCode(socialError)}
        </p>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="card space-y-4 p-6">
        <div>
          <label className="label" htmlFor="email">
            이메일
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            className={`input ${errors.email ? 'input-error' : ''}`}
            {...register('email')}
          />
          {errors.email && <p className="field-error">{errors.email.message}</p>}
        </div>

        <div>
          <label className="label" htmlFor="password">
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            className={`input ${errors.password ? 'input-error' : ''}`}
            {...register('password')}
          />
          {errors.password && <p className="field-error">{errors.password.message}</p>}
        </div>

        {errors.root && (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
            {errors.root.message}
          </p>
        )}

        <button type="submit" className="btn-primary w-full" disabled={isSubmitting}>
          {isSubmitting && <Spinner />}
          로그인
        </button>

        <div className="flex items-center gap-3 text-xs text-slate-400">
          <span className="h-px flex-1 bg-slate-200" />
          또는
          <span className="h-px flex-1 bg-slate-200" />
        </div>

        <div className="space-y-2">
          <button
            type="button"
            className="btn w-full bg-[#FEE500] text-[#191600] hover:brightness-95"
            onClick={() => social('kakao')}
          >
            카카오로 계속하기
          </button>
          <button type="button" className="btn-secondary w-full" onClick={() => social('google')}>
            구글로 계속하기
          </button>
        </div>

        <p className="text-center text-sm text-slate-500">
          계정이 없으신가요?{' '}
          <Link to="/signup" className="font-medium text-brand-600 hover:underline">
            회원가입
          </Link>
        </p>
      </form>
    </div>
  );
}
