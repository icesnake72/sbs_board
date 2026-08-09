import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { usePost, useDeletePost } from '@/hooks/queries';
import { reactToPost } from '@/api/reaction';
import { formatCount, formatDateTime } from '@/lib/format';
import { isNotFound, toMessage } from '@/lib/errorMessage';
import { toast } from '@/stores/toast';
import ImageGallery from '@/components/ImageGallery';
import ReactionButtons from '@/components/ReactionButtons';
import CommentSection from '@/components/CommentSection';
import { ErrorState, Skeleton } from '@/components/ui';
import NotFoundPage from './NotFoundPage';

export default function PostDetailPage() {
  const { postId } = useParams();
  const id = Number(postId);
  const navigate = useNavigate();
  const location = useLocation();
  const { data: post, isLoading, isError, error, refetch } = usePost(id);
  const deleteMutation = useDeletePost();

  // ⚠️ PostDTO.board 는 게시판 "이름" 문자열이라 boardId 를 알 수 없다.
  //    목록에서 넘겨준 state 로 뒤로가기 링크를 유지한다.
  const boardId = location.state?.boardId;

  if (isLoading) {
    return (
      <div className="card space-y-4 p-6">
        <Skeleton className="h-7 w-2/3" />
        <Skeleton className="h-4 w-1/3" />
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  if (isNotFound(error)) return <NotFoundPage message="삭제되었거나 존재하지 않는 게시글입니다" />;
  if (isError) return <ErrorState message={toMessage(error)} onRetry={refetch} />;

  const handleDelete = async () => {
    if (!window.confirm('게시글을 삭제할까요? 되돌릴 수 없습니다.')) return;
    try {
      await deleteMutation.mutateAsync(id);
      toast.success('게시글을 삭제했습니다');
      navigate(boardId ? `/boards/${boardId}` : '/');
    } catch (e) {
      // 댓글이 달린 글은 FK 제약으로 삭제가 실패할 수 있다.
      toast.error(toMessage(e));
    }
  };

  return (
    <>
      <article className="card p-4 sm:p-6">
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <p className="text-sm text-brand-600">
              {boardId ? (
                <Link to={`/boards/${boardId}`} className="hover:underline">
                  {post.board}
                </Link>
              ) : (
                post.board
              )}
            </p>
            <h1 className="mt-1 break-words text-xl font-bold sm:text-2xl">{post.title}</h1>
            <p className="mt-2 flex flex-wrap items-center gap-x-2 text-sm text-slate-500">
              <span>{post.author}</span>
              <span aria-hidden>·</span>
              <span>{formatDateTime(post.createdAt)}</span>
              <span aria-hidden>·</span>
              <span>조회 {formatCount(post.viewCount)}</span>
            </p>
          </div>

          {/* canEdit/canDelete 는 로그인 상태로 상세를 조회했을 때만 정상 계산된다. */}
          <div className="flex shrink-0 gap-2">
            {post.canEdit && (
              <Link to={`/posts/${id}/edit`} state={{ boardId }} className="btn-secondary">
                수정
              </Link>
            )}
            {post.canDelete && (
              <button
                type="button"
                className="btn-danger"
                onClick={handleDelete}
                disabled={deleteMutation.isPending}
              >
                삭제
              </button>
            )}
          </div>
        </div>

        <div className="mt-6 whitespace-pre-wrap break-words leading-relaxed text-slate-800">
          {post.body}
        </div>

        {post.images?.length > 0 && (
          <div className="mt-6">
            <ImageGallery images={post.images} />
          </div>
        )}

        <div className="mt-6 flex justify-center border-t border-slate-100 pt-5">
          <ReactionButtons
            value={{
              likeCount: post.like,
              dislikeCount: post.disLike,
              myReaction: post.myReaction,
            }}
            onReact={(type) => reactToPost(id, type)}
          />
        </div>
      </article>

      <CommentSection postId={id} />
    </>
  );
}
