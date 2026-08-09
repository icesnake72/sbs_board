import { api } from '@/lib/api';
import { PAGE_SIZE } from '@/lib/constants';

/**
 * 최상위 댓글만 페이징된다. 대댓글은 각 항목의 children 에 전부 포함된다.
 * → page.totalElements 는 "전체 댓글 수"가 아니라 "최상위 댓글 수"다.
 */
export const getComments = (postId, page = 0, size = PAGE_SIZE, sort = 'createdAt,ASC') =>
  api.get(
    `/api/comment/post/${postId}/list?${new URLSearchParams({ page: String(page), size: String(size), sort })}`,
  );

export const createComment = (postId, content, parentId) =>
  api.post(`/api/comment/post/${postId}/new`, parentId ? { content, parentId } : { content });

export const updateComment = (id, content) => api.put(`/api/comment/${id}`, { content });

/** soft delete — 목록에서는 deleted: true 로 남는다. */
export const deleteComment = (id) => api.delete(`/api/comment/${id}`);
