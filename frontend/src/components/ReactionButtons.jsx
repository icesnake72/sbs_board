import { useState } from 'react';
import { REACTION } from '@/lib/constants';
import { useAuthStore } from '@/stores/auth';
import { toast } from '@/stores/toast';
import { toMessage } from '@/lib/errorMessage';
import { Spinner } from './ui';

/**
 * 게시글/댓글 공용 반응 버튼.
 *
 * - 초기 카운트와 내 반응은 조회 응답에 이미 들어있으므로 별도 요청이 필요 없다.
 * - 같은 타입을 다시 누르면 취소(토글)된다.
 * - 연타하면 상태가 뒤집히므로 요청 중에는 비활성화한다.
 *
 * @param {{likeCount:number, dislikeCount:number, myReaction:string|null}} value
 * @param {(type:string)=>Promise<object>} onReact ReactionResponse 를 반환
 */
export default function ReactionButtons({ value, onReact, size = 'md' }) {
  const loggedIn = useAuthStore((s) => !!s.accessToken);
  const [pending, setPending] = useState(null);
  const [local, setLocal] = useState(null);

  const state = local ?? value;
  const busy = pending !== null;

  const handle = async (type) => {
    if (!loggedIn) {
      toast.info('로그인이 필요합니다');
      return;
    }
    if (busy) return;

    setPending(type);
    try {
      // 응답으로 해당 항목만 갱신한다 (전체 재조회 불필요).
      const res = await onReact(type);
      setLocal(res);
    } catch (e) {
      toast.error(toMessage(e));
    } finally {
      setPending(null);
    }
  };

  const base =
    size === 'sm'
      ? 'gap-1 rounded-full border px-2 py-0.5 text-xs'
      : 'gap-1.5 rounded-full border px-3.5 py-1.5 text-sm';

  const cls = (active) =>
    `inline-flex items-center font-medium transition disabled:opacity-60 ${base} ${
      active
        ? 'border-brand-500 bg-brand-50 text-brand-700'
        : 'border-slate-300 bg-white text-slate-600 hover:bg-slate-50'
    }`;

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        className={cls(state.myReaction === REACTION.LIKE)}
        disabled={busy}
        aria-pressed={state.myReaction === REACTION.LIKE}
        onClick={() => handle(REACTION.LIKE)}
      >
        {pending === REACTION.LIKE ? <Spinner className="h-3 w-3" /> : <span aria-hidden>👍</span>}
        <span>{state.likeCount ?? 0}</span>
      </button>

      <button
        type="button"
        className={cls(state.myReaction === REACTION.DISLIKE)}
        disabled={busy}
        aria-pressed={state.myReaction === REACTION.DISLIKE}
        onClick={() => handle(REACTION.DISLIKE)}
      >
        {pending === REACTION.DISLIKE ? <Spinner className="h-3 w-3" /> : <span aria-hidden>👎</span>}
        <span>{state.dislikeCount ?? 0}</span>
      </button>
    </div>
  );
}
