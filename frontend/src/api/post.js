import { api } from '@/lib/api';
import { PAGE_SIZE } from '@/lib/constants';

export const getPosts = (boardId, page = 0, size = PAGE_SIZE, sort = 'createdAt,DESC') =>
  api.get(
    `/api/post/${boardId}/all?${new URLSearchParams({ page: String(page), size: String(size), sort })}`,
  );

/** 로그인 상태로 호출해야 canEdit/canDelete/myReaction 이 채워지고 조회수도 증가한다. */
export const getPost = (id) => api.get(`/api/post/${id}`);

/** 게시글 작성 — multipart/form-data. Content-Type 은 직접 설정하지 않는다. */
export const createPost = (boardId, post, images = []) => {
  const fd = new FormData();
  fd.append('post', new Blob([JSON.stringify(post)], { type: 'application/json' }));
  images.forEach((f) => fd.append('images', f)); // 전송 순서 = sortOrder
  return api.post(`/api/post/${boardId}/new`, fd);
};

/** 제목/본문만 수정 가능하다. 이미지는 API 가 지원하지 않는다. */
export const updatePost = (id, post) => api.put(`/api/post/${id}/update`, post);

/** ⚠️ 응답이 text/plain "ok" */
export const deletePost = (id) => api.delete(`/api/post/${id}`);
