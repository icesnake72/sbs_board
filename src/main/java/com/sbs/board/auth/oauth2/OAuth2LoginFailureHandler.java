package com.sbs.board.auth.oauth2;

import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.OAuth2DuplicateEmailException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 실패도 브라우저 전체 페이지 이동 중에 발생하므로 JSON 을 출력하면 원문이 노출된다.
 * 프론트의 /login?error=CODE 로 리다이렉트하여 SPA 가 안내 문구를 띄우게 한다.
 * 성공 핸들러와 같은 이유로 Location 은 상대 경로다.
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        ErrorCode errorCode = ErrorCode.LOGIN_FAILED;

        if (exception instanceof OAuth2DuplicateEmailException) {
            errorCode = ErrorCode.DUPLICATE_USER_EMAIL;
        }

        log.debug("onAuthenticationFailure()::{}", errorCode, exception);

        OAuth2LoginSuccessHandler.redirect(response, "/login?error=" + errorCode.name());
    }
}
