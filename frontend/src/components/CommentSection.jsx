import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useComments, useCommentMutations } from '@/hooks/queries';
import { useAuthStore } from '@/stores/auth';
import { toMessage } from '@/lib/errorMessage';
import CommentItem from './CommentItem';
import CommentForm from './CommentForm';
import { EmptyState, ErrorState, Pagination, Skeleton } from './ui';

export default function CommentSection({ postId }) {
  const [page, setPage] = useState(0);
  const loggedIn = useAuthStore((s) => !!s.accessToken);
  const { data, isLoading, isError, error, refetch } = useComments(postId, page);
  const mutations = useCommentMutations(postId);

  return (
    <section className="card mt-6 p-4 sm:p-6">
      <h2 className="mb-4 text-base font-semibold">
        댓글
        {/* page.totalElements 는 최상위 댓글 수다. 대댓글까지 세는 API 는 없다. */}
        {data && <span className="ml-1 text-brand-600">{data.page.totalElements}</span>}
      </h2>

      {loggedIn ? (
        <CommentForm
          onSubmit={async (content) => {
            try {
              await mutations.create.mutateAsync({ content });
            } catch (e) {
              const err = new Error();
              err.userMessage = toMessage(e);
              throw err;
            }
          }}
        />
      ) : (
        <p className="rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-500">
          댓글을 작성하려면{' '}
          <Link to="/login" className="font-medium text-brand-600 hover:underline">
            로그인
          </Link>
          이 필요합니다.
        </p>
      )}

      <div className="mt-4">
        {isLoading ? (
          <div className="space-y-3 py-2">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="space-y-2">
                <Skeleton className="h-3 w-24" />
                <Skeleton className="h-4 w-3/4" />
              </div>
            ))}
          </div>
        ) : isError ? (
          <ErrorState message={toMessage(error)} onRetry={refetch} />
        ) : !data.content.length ? (
          <EmptyState title="아직 댓글이 없습니다" description="첫 댓글을 남겨보세요." />
        ) : (
          <>
            <ul className="divide-y divide-slate-100">
              {data.content.map((c) => (
                <CommentItem key={c.id} comment={c} mutations={mutations} />
              ))}
            </ul>
            <Pagination page={data} onChange={setPage} />
          </>
        )}
      </div>
    </section>
  );
}
