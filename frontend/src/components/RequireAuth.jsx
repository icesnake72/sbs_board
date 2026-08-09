import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/auth';
import { Spinner } from './ui';

/** initialized 전에는 렌더링하지 않는다 — 세션 복원 중 로그인 화면이 깜빡이는 것을 막는다. */
export default function RequireAuth({ children }) {
  const { accessToken, initialized } = useAuthStore();
  const location = useLocation();

  if (!initialized) {
    return (
      <div className="flex justify-center py-20 text-slate-400">
        <Spinner className="h-6 w-6" />
      </div>
    );
  }

  if (!accessToken) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return children;
}
