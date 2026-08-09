import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotifications, useReadNotification, useUnreadCount } from '@/hooks/queries';
import { formatRelative } from '@/lib/format';
import { Spinner } from './ui';

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const navigate = useNavigate();

  const { data: unread } = useUnreadCount();
  const { data: list, isLoading } = useNotifications(0, open); // 열 때만 조회
  const readMutation = useReadNotification();

  useEffect(() => {
    if (!open) return;
    const onClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, [open]);

  const count = unread?.count ?? 0;

  const handleClick = async (n) => {
    setOpen(false);
    if (!n.read) {
      try {
        await readMutation.mutateAsync(n.id);
      } catch {
        /* 읽음 처리 실패는 이동을 막지 않는다 */
      }
    }
    // ⚠️ postId 는 FK 가 아니라 이미 삭제된 게시글일 수 있다 → 상세에서 404 처리됨
    navigate(`/posts/${n.postId}`);
  };

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        aria-label={`알림${count > 0 ? ` ${count}개` : ''}`}
        aria-expanded={open}
        className="relative rounded-lg p-2 text-slate-600 hover:bg-slate-100"
        onClick={() => setOpen((v) => !v)}
      >
        <span aria-hidden className="text-lg">🔔</span>
        {count > 0 && (
          <span className="absolute -right-0.5 -top-0.5 min-w-[18px] rounded-full bg-red-500 px-1 text-center text-[11px] font-semibold leading-[18px] text-white">
            {count > 99 ? '99+' : count}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-40 mt-2 w-80 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg">
          <div className="border-b border-slate-100 px-4 py-2.5 text-sm font-semibold">알림</div>

          {isLoading ? (
            <div className="flex justify-center py-8 text-slate-400">
              <Spinner className="h-5 w-5" />
            </div>
          ) : !list?.content.length ? (
            <p className="px-4 py-8 text-center text-sm text-slate-500">알림이 없습니다</p>
          ) : (
            <ul className="max-h-96 divide-y divide-slate-100 overflow-y-auto">
              {list.content.map((n) => (
                <li key={n.id}>
                  <button
                    type="button"
                    onClick={() => handleClick(n)}
                    className={`block w-full px-4 py-3 text-left text-sm hover:bg-slate-50 ${
                      n.read ? 'text-slate-500' : 'bg-brand-50/60 font-medium text-slate-800'
                    }`}
                  >
                    <p className="line-clamp-2">{n.message}</p>
                    <p className="mt-1 text-xs text-slate-400">{formatRelative(n.createdAt)}</p>
                  </button>
                </li>
              ))}
            </ul>
          )}
          {/* "모두 읽음" 은 서버 API 가 없어 제공하지 않는다. */}
        </div>
      )}
    </div>
  );
}
