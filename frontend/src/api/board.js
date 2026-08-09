import { api } from '@/lib/api';

export const getBoards = () => api.get('/api/board/all');

export const createBoard = (body) => api.post('/api/board/new', body);

export const updateBoard = (id, body) => api.put(`/api/board/${id}/update`, body);

/** ⚠️ 응답이 text/plain "ok" 다. 게시글이 남아 있으면 실패한다. */
export const deleteBoard = (id) => api.delete(`/api/board/${id}`);
