package com.sbs.board.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),

    DUPLICATE_USER_EMAIL(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다."),
    DUPLICATE_BOARD_NAME(HttpStatus.CONFLICT, "이미 존재하는 게시판입니다."),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시글을 작성할 권한이 없습니다."),
    BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시판을 생성할 권한이 없습니다."),

    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "로그인에 실패하였습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_BOARD_ID(HttpStatus.BAD_REQUEST, "해당 Board ID는 삭제할 수 없습니다."),
    INVALID_OAUTH_STATE(HttpStatus.UNAUTHORIZED, "정상적인 인증 요청이 아닙니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 refresh token입니다. 다시 로그인하세요"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "잘못된 refresh token입니다."),


    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일형식 입니다."),
    INVALID_FILE_UPLOAD_DIR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 디렉토리 초기화 실패"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 실패"),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 업로드 개수 초과"),
    MAX_UPLOAD_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 용량 초과"),

    CANNOT_REPLY_TO_REPLY(HttpStatus.BAD_REQUEST, "대댓글에 댓글을 달 수 없습니다"),
    CANNOT_REPLY_TO_DELETED(HttpStatus.BAD_REQUEST, "삭제된 댓글에 댓글을 달 수 없습니다"),
    CANNOT_EDIT_DELETED(HttpStatus.BAD_REQUEST, "삭제된 댓글은 수정할 수 없습니다"),
    COMMENT_POST_MISMATCH(HttpStatus.BAD_REQUEST, "부모 댓글이 해당 게시글의 댓글이 아닙니다"),

    CANNOT_VIEW_NOTIFICATION(HttpStatus.FORBIDDEN, "해당 알림의 소유자만 조회 가능합니다"),

    METHOD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 요청 방식입니다."),
    SQL_INTEGRITY_ERROR(HttpStatus.BAD_REQUEST, "데이터베이스 참조 무결성 위배 에러입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 내부 에러가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
