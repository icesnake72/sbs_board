import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, useParams } from 'react-router-dom';
import { postSchema } from '@/lib/schemas';
import { useCreatePost } from '@/hooks/queries';
import { toMessage } from '@/lib/errorMessage';
import { toast } from '@/stores/toast';
import ImageUploader from '@/components/ImageUploader';
import { Spinner } from '@/components/ui';

export default function PostNewPage() {
  const { boardId } = useParams();
  const id = Number(boardId);
  const navigate = useNavigate();
  const [images, setImages] = useState([]);
  const createMutation = useCreatePost(id);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(postSchema), defaultValues: { title: '', body: '' } });

  const onSubmit = async (values) => {
    try {
      const created = await createMutation.mutateAsync({ post: values, images });
      toast.success('게시글을 등록했습니다');
      navigate(`/posts/${created.id}`, { replace: true, state: { boardId: id } });
    } catch (e) {
      toast.error(toMessage(e));
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="card space-y-5 p-4 sm:p-6">
      <h1 className="text-xl font-bold">글쓰기</h1>

      <div>
        <label className="label" htmlFor="title">
          제목
        </label>
        <input
          id="title"
          className={`input ${errors.title ? 'input-error' : ''}`}
          placeholder="제목을 입력하세요 (5~200자)"
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
          placeholder="내용을 입력하세요"
          {...register('body')}
        />
        {errors.body && <p className="field-error">{errors.body.message}</p>}
      </div>

      <div>
        <span className="label">이미지</span>
        <ImageUploader files={images} onChange={setImages} disabled={isSubmitting} />
      </div>

      <div className="flex justify-end gap-2">
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          취소
        </button>
        <button type="submit" className="btn-primary" disabled={isSubmitting}>
          {isSubmitting && <Spinner />}
          등록
        </button>
      </div>
    </form>
  );
}
