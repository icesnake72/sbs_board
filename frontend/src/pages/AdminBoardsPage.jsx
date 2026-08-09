import { useState } from 'react';
import { useBoards, useBoardMutations } from '@/hooks/queries';
import { boardSchema } from '@/lib/schemas';
import { isForbidden, toMessage } from '@/lib/errorMessage';
import { toast } from '@/stores/toast';
import { formatDate } from '@/lib/format';
import { ErrorState, Skeleton, Spinner } from '@/components/ui';

/**
 * 🚧 서버가 role 을 내려주지 않아(항상 null) ADMIN 여부를 알 수 없다.
 *    그래서 이 화면은 헤더 메뉴에 노출하지 않고 직접 URL 로만 접근한다.
 *    권한이 없으면 서버가 403 을 주고, 아래에서 접근 거부로 처리한다.
 */
export default function AdminBoardsPage() {
  const { data: boards, isLoading, isError, error, refetch } = useBoards();
  const { create, update, remove } = useBoardMutations();

  const [form, setForm] = useState({ name: '', description: '' });
  const [editingId, setEditingId] = useState(null);
  const [formError, setFormError] = useState('');

  const reset = () => {
    setForm({ name: '', description: '' });
    setEditingId(null);
    setFormError('');
  };

  const submit = async (e) => {
    e.preventDefault();
    const parsed = boardSchema.safeParse(form);
    if (!parsed.success) {
      setFormError(parsed.error.issues[0].message);
      return;
    }
    setFormError('');

    try {
      if (editingId) {
        await update.mutateAsync({ id: editingId, body: parsed.data });
        toast.success('게시판을 수정했습니다');
      } else {
        await create.mutateAsync(parsed.data);
        toast.success('게시판을 생성했습니다');
      }
      reset();
    } catch (err) {
      if (isForbidden(err)) {
        setFormError('권한이 없습니다. 관리자 계정으로 로그인하세요.');
        return;
      }
      setFormError(toMessage(err));
    }
  };

  const handleDelete = async (board) => {
    if (!window.confirm(`"${board.name}" 게시판을 삭제할까요?`)) return;
    try {
      await remove.mutateAsync(board.id);
      toast.success('게시판을 삭제했습니다');
    } catch (err) {
      // 게시글이 남아 있으면 FK 제약으로 실패한다.
      toast.error(toMessage(err));
    }
  };

  if (isLoading) return <Skeleton className="h-64 w-full" />;
  if (isError) return <ErrorState message={toMessage(error)} onRetry={refetch} />;

  const busy = create.isPending || update.isPending;

  return (
    <>
      <h1 className="mb-1 text-xl font-bold">게시판 관리</h1>
      <p className="mb-4 text-sm text-slate-500">
        ADMIN 권한이 필요합니다. 권한이 없으면 저장 시 403이 반환됩니다.
      </p>

      <form onSubmit={submit} className="card mb-6 space-y-3 p-4 sm:p-6">
        <h2 className="font-semibold">{editingId ? '게시판 수정' : '새 게시판'}</h2>

        <div>
          <label className="label" htmlFor="board-name">
            이름
          </label>
          <input
            id="board-name"
            className="input"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
          />
        </div>

        <div>
          <label className="label" htmlFor="board-desc">
            설명
          </label>
          <input
            id="board-desc"
            className="input"
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
          />
        </div>

        {formError && <p className="field-error">{formError}</p>}

        <div className="flex justify-end gap-2">
          {editingId && (
            <button type="button" className="btn-secondary" onClick={reset}>
              취소
            </button>
          )}
          <button type="submit" className="btn-primary" disabled={busy}>
            {busy && <Spinner />}
            {editingId ? '저장' : '생성'}
          </button>
        </div>
      </form>

      <ul className="card divide-y divide-slate-100">
        {boards.map((b) => (
          <li key={b.id} className="flex items-center gap-3 px-4 py-3">
            <div className="min-w-0 flex-1">
              <p className="font-medium">{b.name}</p>
              <p className="truncate text-sm text-slate-500">
                {b.description || '설명 없음'} · {formatDate(b.createdAt)}
              </p>
            </div>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => {
                setEditingId(b.id);
                setForm({ name: b.name, description: b.description ?? '' });
                setFormError('');
              }}
            >
              수정
            </button>
            <button type="button" className="btn-danger" onClick={() => handleDelete(b)}>
              삭제
            </button>
          </li>
        ))}
      </ul>
    </>
  );
}
