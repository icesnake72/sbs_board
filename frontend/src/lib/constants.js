/** Page<T> 헬퍼 — 서버는 VIA_DTO 모드라 { content, page: {...} } 구조다. */
export const isLastPage = (p) => !p || p.page.number + 1 >= p.page.totalPages;
export const isFirstPage = (p) => !p || p.page.number === 0;

/** 삭제된 댓글의 content 는 서버가 이 문자열로 바꿔서 내려준다. */
export const DELETED_COMMENT_CONTENT = '삭제된 게시글입니다';

export const ALLOWED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];
export const MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB
export const MAX_IMAGE_COUNT = 5;

export const PAGE_SIZE = 10;

export const REACTION = { LIKE: 'LIKE', DISLIKE: 'DISLIKE' };
