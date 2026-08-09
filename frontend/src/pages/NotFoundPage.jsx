import { Link } from 'react-router-dom';

export default function NotFoundPage({ message = '삭제되었거나 존재하지 않습니다' }) {
  return (
    <div className="flex flex-col items-center gap-3 py-24 text-center">
      <p className="text-4xl" aria-hidden>
        🔍
      </p>
      <p className="text-slate-600">{message}</p>
      <Link to="/" className="btn-secondary">
        홈으로
      </Link>
    </div>
  );
}
