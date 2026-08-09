import { ApiError } from './api';

/** 서버 message 보다 사용자 친화적인 문구가 필요한 코드만 덮어쓴다. */
const OVERRIDE = {
  LOGIN_REQUIRED: '이메일 또는 비밀번호가 올바르지 않습니다',
  DUPLICATE_USER_EMAIL: '이미 사용 중인 이메일입니다',
  DUPLICATE_BOARD_NAME: '이미 존재하는 게시판 이름입니다',
  CANNOT_REPLY_TO_REPLY: '대댓글에는 답글을 달 수 없습니다',
  CANNOT_REPLY_TO_DELETED: '삭제된 댓글에는 답글을 달 수 없습니다',
  CANNOT_EDIT_DELETED: '삭제된 댓글은 수정할 수 없습니다',
  SQL_INTEGRITY_ERROR: '연결된 데이터가 있어 삭제할 수 없습니다',
  ACCESS_DENIED: '권한이 없습니다',
  INVALID_INPUT: '입력값을 다시 확인해 주세요',
  UNKNOWN: '알 수 없는 오류가 발생했습니다',
};

export function toMessage(e) {
  if (e instanceof ApiError) return OVERRIDE[e.code] ?? e.message;
  return '네트워크 오류가 발생했습니다';
}

/** ErrorCode 문자열만 있을 때 쓴다 — 예: 소셜 로그인 실패 리다이렉트의 ?error=CODE */
const CODE_MESSAGE = {
  ...OVERRIDE,
  LOGIN_FAILED: '소셜 로그인에 실패했습니다',
  DUPLICATE_USER_EMAIL: '이미 같은 이메일로 가입된 계정이 있습니다',
  INVALID_OAUTH_STATE: '정상적인 인증 요청이 아닙니다. 다시 시도해 주세요',
};

export const messageForCode = (code) => CODE_MESSAGE[code] ?? CODE_MESSAGE.UNKNOWN;

/** 404 여부 — "삭제되었거나 존재하지 않습니다" 화면 분기에 쓴다. */
export const isNotFound = (e) => e instanceof ApiError && e.status === 404;
export const isForbidden = (e) => e instanceof ApiError && e.status === 403;
