import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import { signupSchema } from '@/lib/schemas';
import { signup as signupApi } from '@/api/auth';
import { ApiError } from '@/lib/api';
import { toMessage } from '@/lib/errorMessage';
import { toast } from '@/stores/toast';
import { Spinner } from '@/components/ui';

export default function SignupPage() {
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(signupSchema),
    defaultValues: { email: '', password: '', passwordConfirm: '', nickName: '' },
  });

  const onSubmit = async (values) => {
    try {
      // 서버는 이메일 형식을 검증하지 않는다 → 위 zod 스키마가 유일한 방어선이다.
      await signupApi(values);
      toast.success('가입이 완료되었습니다. 로그인해 주세요.');
      navigate('/login', { replace: true });
    } catch (e) {
      if (e instanceof ApiError && e.code === 'DUPLICATE_USER_EMAIL') {
        setError('email', { message: '이미 사용 중인 이메일입니다' });
        return;
      }
      setError('root', { message: toMessage(e) });
    }
  };

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-6 text-center text-2xl font-bold">회원가입</h1>

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
          <label className="label" htmlFor="nickName">
            닉네임
          </label>
          <input
            id="nickName"
            className={`input ${errors.nickName ? 'input-error' : ''}`}
            {...register('nickName')}
          />
          {errors.nickName && <p className="field-error">{errors.nickName.message}</p>}
        </div>

        <div>
          <label className="label" htmlFor="password">
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            placeholder="8~30자"
            className={`input ${errors.password ? 'input-error' : ''}`}
            {...register('password')}
          />
          {errors.password && <p className="field-error">{errors.password.message}</p>}
        </div>

        <div>
          <label className="label" htmlFor="passwordConfirm">
            비밀번호 확인
          </label>
          <input
            id="passwordConfirm"
            type="password"
            autoComplete="new-password"
            className={`input ${errors.passwordConfirm ? 'input-error' : ''}`}
            {...register('passwordConfirm')}
          />
          {errors.passwordConfirm && (
            <p className="field-error">{errors.passwordConfirm.message}</p>
          )}
        </div>

        {errors.root && (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
            {errors.root.message}
          </p>
        )}

        <button type="submit" className="btn-primary w-full" disabled={isSubmitting}>
          {isSubmitting && <Spinner />}
          가입하기
        </button>

        <p className="text-center text-sm text-slate-500">
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            로그인
          </Link>
        </p>
      </form>
    </div>
  );
}
