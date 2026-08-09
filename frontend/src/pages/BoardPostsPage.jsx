import { useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useBoards, usePosts } from '@/hooks/queries';
import { useAuthStore } from '@/stores/auth';
import { formatRelative, formatCount } from '@/lib/format';
import { toMessage } from '@/lib/errorMessage';
import { imageUrl } from '@/lib/api';
import { EmptyState, ErrorState, Pagination, PostListSkeleton } from '@/components/ui';
import NotFoundPage from './NotFoundPage';

const SORTS = [
  { value: 'createdAt,DESC', label: '최신순' },
  { value: 'viewCount,DESC', label: '조회순' },
];

export default function BoardPostsPage() {
  const { boardId } = useParams();
  const id = Number(boardId);
  const [params, setParams] = useSearchParams();
  const [sort, setSort] = useState(SORTS[0].value);
  const loggedIn = useAuthStore((s) => !!s.accessToken);

  const page = Number(params.get('page') ?? 0);
  const { data: boards, isLoading: boardsLoading } = useBoards();
  const { data, isLoading, isError, error, refetch } = usePosts(id, page, sort);

  // 없는 boardId 도 빈 페이지가 오므로, 게시판 존재 여부는 /api/board/all 로 확인한다.
  const board = boards?.find((b) => b.id === id);
  if (!boardsLoading && boards && !board) return <NotFoundPage message="존재하지 않는 게시판입니다" />;

  const setPage = (p) => setParams({ page: String(p) });

  return (
    <>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="flex-1">
          <h1 className="text-xl font-bold">{board?.name ?? '게시판'}</h1>
          {board?.description && <p className="text-sm text-slate-500">{board.description}</p>}
        </div>

        <select
          className="input w-auto"
          value={sort}
          aria-label="정렬"
          onChange={(e) => {
            setSort(e.target.value);
            setPage(0);
          }}
        >
          {SORTS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>

        {loggedIn && (
          <Link to={`/boards/${id}/posts/new`} className="btn-primary">
            글쓰기
          </Link>
        )}
      </div>

      <div className="card overflow-hidden">
        {isLoading ? (
          <PostListSkeleton />
        ) : isError ? (
          <ErrorState message={toMessage(error)} onRetry={refetch} />
        ) : !data.content.length ? (
          <EmptyState
            title="아직 게시글이 없습니다"
            description="첫 글을 작성해 보세요."
            action={
              loggedIn ? (
                <Link to={`/boards/${id}/posts/new`} className="btn-primary">
                  글쓰기
                </Link>
              ) : (
                <Link to="/login" className="btn-secondary">
                  로그인
                </Link>
              )
            }
          />
        ) : (
          <ul className="divide-y divide-slate-100">
            {/* ⚠️ 목록 응답에도 body 가 전부 들어있지만 렌더링하지 않는다 (성능). */}
            {data.content.map((p) => (
              <li key={p.id}>
                <Link
                  to={`/posts/${p.id}`}
                  state={{ boardId: id }}
                  className="flex items-center gap-3 px-4 py-3.5 hover:bg-slate-50"
                >
                  {p.images?.length > 0 && (
                    <img
                      src={imageUrl(p.images[0].url)}
                      alt=""
                      loading="lazy"
                      className="h-12 w-12 shrink-0 rounded-lg object-cover"
                    />
                  )}
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium text-slate-800">{p.title}</p>
                    <p className="mt-0.5 flex flex-wrap items-center gap-x-2 text-xs text-slate-500">
                      <span>{p.author}</span>
                      <span aria-hidden>·</span>
                      <span>{formatRelative(p.createdAt)}</span>
                      <span aria-hidden>·</span>
                      <span>조회 {formatCount(p.viewCount)}</span>
                      {p.like > 0 && (
                        <>
                          <span aria-hidden>·</span>
                          <span>👍 {formatCount(p.like)}</span>
                        </>
                      )}
                    </p>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>

      {data && <Pagination page={data} onChange={setPage} />}
    </>
  );
}
