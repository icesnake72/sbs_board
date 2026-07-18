package com.sbs.board.global.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public class OAuth2DuplicateEmailException extends OAuth2AuthenticationException {

    public OAuth2DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_USER_EMAIL.getMessage());
    }
}
