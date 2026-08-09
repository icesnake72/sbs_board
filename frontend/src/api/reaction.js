import { api } from '@/lib/api';

/** ⚠️ 게시글 경로는 서버 오타로 "reation" 이다. 댓글은 "reaction" 으로 정상. */
export const reactToPost = (postId, type) => api.post(`/api/post/${postId}/reation`, { type });

export const reactToComment = (commentId, type) =>
  api.post(`/api/comment/${commentId}/reaction`, { type });
