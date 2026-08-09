import { useToastStore } from '@/stores/toast';
import { isLastPage, isFirstPage } from '@/lib/constants';

export function Spinner({ className = 'h-4 w-4' }) {
  return (
    <svg className={`animate-spin ${className}`} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
    </svg>
  );
}

export function Skeleton({ className = '' }) {
  return <div className={`animate-pulse rounded bg-slate-200 ${className}`} />;
}

export function PostListSkeleton({ count = 5 }) {
  return (
    <ul className="divide-y divide-slate-200">
      {Array.from({ length: count }).map((_, i) => (
        <li key={i} className="space-y-2 px-4 py-4">
          <Skeleton className="h-5 w-2/3" />
          <Skeleton className="h-3 w-1/3" />
        </li>
      ))}
    </ul>
  );
}

export function EmptyState({ title, description, action }) {
  return (
    <div className="flex flex-col items-center gap-2 px-4 py-16 text-center">
      <p className="text-sm font-medium text-slate-700">{title}</p>
      {description && <p className="text-sm text-slate-500">{description}</p>}
      {action && <div className="mt-3">{action}</div>}
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="flex flex-col items-center gap-3 px-4 py-16 text-center">
      <p className="text-sm text-slate-600">{message}</p>
      {onRetry && (
        <button type="button" className="btn-secondary" onClick={onRetry}>
          다시 시도
        </button>
      )}
    </div>
  );
}

/** 0-based 페이지네이션. Page<T> 를 그대로 받는다. */
export function Pagination({ page, onChange }) {
  if (!page || page.page.totalPages <= 1) return null;

  const current = page.page.number;
  const total = page.page.totalPages;

  // 현재 페이지 주변 최대 5개만 노출
  const start = Math.max(0, Math.min(current - 2, total - 5));
  const end = Math.min(total, start + 5);
  const numbers = Array.from({ length: end - start }, (_, i) => start + i);

  return (
    <nav className="flex items-center justify-center gap-1 py-6" aria-label="페이지">
      <button
        type="button"
        className="btn-ghost px-2"
        disabled={isFirstPage(page)}
        onClick={() => onChange(current - 1)}
      >
        이전
      </button>
      {numbers.map((n) => (
        <button
          key={n}
          type="button"
          aria-current={n === current ? 'page' : undefined}
          className={
            n === current
              ? 'btn bg-brand-500 px-3 text-white'
              : 'btn-ghost px-3'
          }
          onClick={() => onChange(n)}
        >
          {n + 1}
        </button>
      ))}
      <button
        type="button"
        className="btn-ghost px-2"
        disabled={isLastPage(page)}
        onClick={() => onChange(current + 1)}
      >
        다음
      </button>
    </nav>
  );
}

const TOAST_STYLE = {
  info: 'bg-slate-800 text-white',
  success: 'bg-emerald-600 text-white',
  error: 'bg-red-600 text-white',
};

export function Toaster() {
  const toasts = useToastStore((s) => s.toasts);
  const dismiss = useToastStore((s) => s.dismiss);

  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4">
      {toasts.map((t) => (
        <button
          key={t.id}
          type="button"
          onClick={() => dismiss(t.id)}
          className={`pointer-events-auto max-w-sm rounded-lg px-4 py-2.5 text-sm shadow-lg ${TOAST_STYLE[t.type]}`}
        >
          {t.message}
        </button>
      ))}
    </div>
  );
}
