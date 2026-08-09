import { useState } from 'react';
import { useAuthStore } from '@/stores/auth';
import { toast } from '@/stores/toast';
import { toMessage } from '@/lib/errorMessage';
import { formatRelative } from '@/lib/format';
import { reactToComment } from '@/api/reaction';
import ReactionButtons from './ReactionButtons';
import CommentForm from './CommentForm';

/**
 * 댓글 1건 + 대댓글.
 *
 * ⚠️ CommentResponse 에 작성자 ID 가 없어 내 댓글 판별이 닉네임 문자열 비교뿐이다.
 *    중복 닉네임에서는 오작동한다 (서버에 authorId 또는 canEdit/canDelete 추가 요청 필요).
 * ⚠️ 깊이 2 제한이라 대댓글에는 답글 버튼을 노출하지 않는다.
 */
export default function CommentItem({ comment, isReply = false, mutations }) {
  const myNickName = useAuthStore((s) => s.user?.nickName);
  const loggedIn = useAuthStore((s) => !!s.accessToken);
  const [replying, setReplying] = useState(false);
  const [editing, setEditing] = useState(false);

  const isMine = !!myNickName && myNickName === comment.authorUsername;
  const canAct = !comment.deleted && isMine;

  const handleDelete = async () => {
    if (!window.confirm('댓글을 삭제할까요?')) return;
    try {
      await mutations.remove.mutateAsync(comment.id);
      toast.success('댓글을 삭제했습니다');
    } catch (e) {
      toast.error(toMessage(e));
    }
  };

  return (
    <li className={isReply ? 'border-l-2 border-slate-100 pl-4' : ''}>
      <div className={`py-3 ${comment.deleted ? 'text-slate-400' : ''}`}>
        <div className="flex items-center gap-2 text-sm">
          <span className={`font-medium ${comment.deleted ? 'text-slate-400' : 'text-slate-800'}`}>
            {comment.authorUsername}
          </span>
          {isMine && !comment.deleted && (
            <span className="rounded bg-brand-50 px-1.5 py-0.5 text-[11px] text-brand-700">
              내 댓글
            </span>
          )}
          <span className="text-xs text-slate-400">{formatRelative(comment.createdAt)}</span>
        </div>

        {editing ? (
          <div className="mt-2">
            <CommentForm
              initialValue={comment.content}
              submitLabel="수정"
              autoFocus
              onCancel={() => setEditing(false)}
              onSubmit={async (content) => {
                try {
                  await mutations.update.mutateAsync({ id: comment.id, content });
                  setEditing(false);
                  toast.success('댓글을 수정했습니다');
                } catch (e) {
                  const err = new Error();
                  err.userMessage = toMessage(e);
                  throw err;
                }
              }}
            />
          </div>
        ) : (
          <p className="mt-1 whitespace-pre-wrap break-words text-sm">{comment.content}</p>
        )}

        {/* 삭제된 댓글에는 답글·수정·반응 버튼을 노출하지 않는다. */}
        {!comment.deleted && !editing && (
          <div className="mt-2 flex flex-wrap items-center gap-3">
            <ReactionButtons
              size="sm"
              value={comment}
              onReact={(type) => reactToComment(comment.id, type)}
            />

            {!isReply && loggedIn && (
              <button
                type="button"
                className="text-xs text-slate-500 hover:text-slate-700"
                onClick={() => setReplying((v) => !v)}
              >
                답글
              </button>
            )}

            {canAct && (
              <>
                <button
                  type="button"
                  className="text-xs text-slate-500 hover:text-slate-700"
                  onClick={() => setEditing(true)}
                >
                  수정
                </button>
                <button
                  type="button"
                  className="text-xs text-red-500 hover:text-red-600"
                  onClick={handleDelete}
                >
                  삭제
                </button>
              </>
            )}
          </div>
        )}

        {replying && (
          <div className="mt-3">
            <CommentForm
              placeholder="답글을 입력하세요"
              submitLabel="답글 등록"
              autoFocus
              onCancel={() => setReplying(false)}
              onSubmit={async (content) => {
                try {
                  await mutations.create.mutateAsync({ content, parentId: comment.id });
                  setReplying(false);
                } catch (e) {
                  const err = new Error();
                  err.userMessage = toMessage(e);
                  throw err;
                }
              }}
            />
          </div>
        )}
      </div>

      {comment.children?.length > 0 && (
        <ul className="ml-2 divide-y divide-slate-100">
          {comment.children.map((child) => (
            <CommentItem key={child.id} comment={child} isReply mutations={mutations} />
          ))}
        </ul>
      )}
    </li>
  );
}
