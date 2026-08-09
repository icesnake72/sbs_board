import { Link } from 'react-router-dom';
import { useBoards, usePosts } from '@/hooks/queries';
import { formatRelative } from '@/lib/format';
import { toMessage } from '@/lib/errorMessage';
import { EmptyState, ErrorState, Skeleton } from '@/components/ui';

/** 게시판별 최근 5건. GET /api/post/all 은 페이징이 없고 본문을 전부 포함해 사용하지 않는다. */
function BoardCard({ board }) {
  const { data, isLoading } = usePosts(board.id, 0, 'createdAt,DESC');
  const posts = data?.content.slice(0, 5) ?? [];

  return (
    <li className="card p-5">
      <Link to={`/boards/${board.id}`} className="group">
        <h2 className="text-base font-semibold group-hover:text-brand-600">{board.name}</h2>
        {board.description && (
          <p className="mt-0.5 line-clamp-1 text-sm text-slate-500">{board.description}</p>
        )}
      </Link>

      <ul className="mt-3 space-y-1.5 border-t border-slate-100 pt-3">
        {isLoading ? (
          Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-4 w-full" />)
        ) : !posts.length ? (
          <li className="text-sm text-slate-400">아직 게시글이 없습니다</li>
        ) : (
          posts.map((p) => (
            <li key={p.id} className="flex items-center gap-2 text-sm">
              <Link
                to={`/posts/${p.id}`}
                state={{ boardId: board.id }}
                className="flex-1 truncate text-slate-700 hover:text-brand-600"
              >
                {p.title}
              </Link>
              {p.images?.length > 0 && <span aria-label="이미지 있음">🖼</span>}
              <span className="shrink-0 text-xs text-slate-400">
                {formatRelative(p.createdAt)}
              </span>
            </li>
          ))
        )}
      </ul>
    </li>
  );
}

export default function HomePage() {
  const { data: boards, isLoading, isError, error, refetch } = useBoards();

  if (isLoading) {
    return (
      <ul className="grid gap-4 sm:grid-cols-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <li key={i} className="card space-y-3 p-5">
            <Skeleton className="h-5 w-1/3" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-2/3" />
          </li>
        ))}
      </ul>
    );
  }

  if (isError) return <ErrorState message={toMessage(error)} onRetry={refetch} />;

  if (!boards?.length) {
    return <EmptyState title="게시판이 없습니다" description="관리자가 게시판을 만들어야 합니다." />;
  }

  return (
    <>
      <h1 className="mb-4 text-xl font-bold">게시판</h1>
      <ul className="grid gap-4 sm:grid-cols-2">
        {boards.map((b) => (
          <BoardCard key={b.id} board={b} />
        ))}
      </ul>
    </>
  );
}
