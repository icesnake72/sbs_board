import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { postSchema } from '@/lib/schemas';
import { usePost, useUpdatePost } from '@/hooks/queries';
import { isNotFound, toMessage } from '@/lib/errorMessage';
import { toast } from '@/stores/toast';
import ImageGallery from '@/components/ImageGallery';
import { ErrorState, Skeleton, Spinner } from '@/components/ui';
import NotFoundPage from './NotFoundPage';

export default function PostEditPage() {
  const { postId } = useParams();
  const id = Number(postId);
  const navigate = useNavigate();
  const location = useLocation();
  const boardId = location.state?.boardId;

  const { data: post, isLoading, isError, error, refetch } = usePost(id);
  const updateMutation = useUpdatePost(id);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(postSchema), defaultValues: { title: '', body: '' } });

  useEffect(() => {
    if (post) reset({ title: post.title, body: post.body });
  }, [post, reset]);

  if (isLoading) {
    return (
      <div className="card space-y-4 p-6">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-48 w-full" />
      </div>
    );
  }

  if (isNotFound(error)) return <NotFoundPage message="삭제되었거나 존재하지 않는 게시글입니다" />;
  if (isError) return <ErrorState message={toMessage(error)} onRetry={refetch} />;

  // 진입 시 권한 확인. canEdit 은 로그인 상태로 조회해야 정상 계산된다.
  if (!post.canEdit) {
    return <NotFoundPage message="이 게시글을 수정할 권한이 없습니다" />;
  }

  const onSubmit = async (values) => {
    try {
      await updateMutation.mutateAsync(values);
      toast.success('게시글을 수정했습니다');
      navigate(`/posts/${id}`, { replace: true, state: { boardId } });
    } catch (e) {
      toast.error(toMessage(e));
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="card space-y-5 p-4 sm:p-6">
      <h1 className="text-xl font-bold">게시글 수정</h1>

      <div>
        <label className="label" htmlFor="title">
          제목
        </label>
        <input
          id="title"
          className={`input ${errors.title ? 'input-error' : ''}`}
          {...register('title')}
        />
        {errors.title && <p className="field-error">{errors.title.message}</p>}
      </div>

      <div>
        <label className="label" htmlFor="body">
          본문
        </label>
        <textarea
          id="body"
          rows={12}
          className={`input resize-y ${errors.body ? 'input-error' : ''}`}
          {...register('body')}
        />
        {errors.body && <p className="field-error">{errors.body.message}</p>}
      </div>

      {/* 이미지 추가/삭제/순서 변경 API 가 없어 읽기 전용으로만 보여준다. */}
      {post.images?.length > 0 && (
        <div>
          <span className="label">첨부된 이미지</span>
          <p className="mb-2 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800">
            이미지는 수정할 수 없습니다.
          </p>
          <ImageGallery images={post.images} />
        </div>
      )}

      <div className="flex justify-end gap-2">
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          취소
        </button>
        <button type="submit" className="btn-primary" disabled={isSubmitting}>
          {isSubmitting && <Spinner />}
          저장
        </button>
      </div>
    </form>
  );
}
