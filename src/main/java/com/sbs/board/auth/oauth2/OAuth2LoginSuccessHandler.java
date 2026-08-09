package com.sbs.board.auth.oauth2;

import com.sbs.board.auth.*;
import com.sbs.board.auth.jwt.JwtTokenProvider;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.UnauthorizedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.sbs.board.global.exception.ErrorCode.LOGIN_REQUIRED;

/**
 * 소셜 로그인 성공 시 JSON 을 출력하면 브라우저에 원문이 그대로 노출되고 SPA 로 돌아갈 수 없다.
 * 그래서 프론트엔드의 /oauth/callback 으로 302 리다이렉트하고, 토큰은 fragment(#) 에 실어 보낸다.
 * fragment 는 서버로 전송되지 않으므로 access log / Referer 에 토큰이 남지 않는다.
 *
 * Location 은 반드시 <b>상대 경로</b>다. 백엔드는 리버스 프록시 뒤(8090, 외부 미노출)에 있고
 * 브라우저는 이 시점에 이미 공개 오리진(nginx:80)에 있으므로, 상대 경로여야 그 오리진 그대로 이동한다.
 * 절대 URL 을 만들면 프록시가 넘긴 Host 헤더에 의존하게 되어 환경마다 깨진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;

        String provider = oauth2Token.getAuthorizedClientRegistrationId();
        String providerId = provider + "_" + authentication.getName();

        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(()-> new UnauthorizedException(ErrorCode.LOGIN_FAILED));


        try {

            String accessToken = jwtTokenProvider.createToken(user.getEmail());
            TokenPair tokenPair = authService.issueRefreshTokenPair(user.getId());

            // Refresh Token 은 그대로 HttpOnly 쿠키로 심는다 (리다이렉트 응답에도 헤더는 유지된다)
            response.addHeader(HttpHeaders.SET_COOKIE,
                    refreshCookieFactory.create(tokenPair.getToken(), tokenPair.getExpireIn()).toString());

            // Access Token / 표시용 정보는 fragment 로 넘긴다. "Bearer " 접두사는 프론트가 붙인다.
            String location = "/oauth/callback"
                    + "#accessToken=" + encode(accessToken)
                    + "&id=" + user.getId()
                    + "&nickName=" + encode(user.getNickName());

            log.debug("onAuthenticationSuccess()::redirect: /oauth/callback (user={})", user.getId());
            redirect(response, location);

        } catch (AuthenticationException ex) {
            throw new UnauthorizedException(LOGIN_REQUIRED);
        }
    }

    /**
     * sendRedirect() 대신 Location 을 직접 세팅한다.
     * 컨테이너 설정(useRelativeRedirects)에 따라 sendRedirect 가 상대 경로를 절대 URL 로
     * 바꿔버릴 수 있는데, 그러면 프록시가 넘긴 Host 를 쓰게 되어 포트가 틀어진다.
     */
    static void redirect(HttpServletResponse response, String location) {
        response.setStatus(HttpServletResponse.SC_FOUND); // 302
        response.setHeader(HttpHeaders.LOCATION, location);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}

// Back-End: http://boardapi.sbs.com/1/new

// CORS: white list, Front-End(http://board.sbs.com)
// CSRF
// XSS
