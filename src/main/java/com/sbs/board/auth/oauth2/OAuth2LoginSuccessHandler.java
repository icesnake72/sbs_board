package com.sbs.board.auth.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbs.board.auth.*;
import com.sbs.board.auth.dto.UserResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.sbs.board.auth.jwt.JwtAuthenticationFilter.BEARER;
import static com.sbs.board.global.exception.ErrorCode.LOGIN_REQUIRED;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;
    private final ObjectMapper objectMapper;

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
            com.sbs.board.auth.TokenPair tokenPair = authService.issueRefreshTokenPair(user.getId());

            UserResponse userResponse = new UserResponse();

            userResponse.setId(user.getId());
            userResponse.setEmail(user.getEmail());
            userResponse.setNickName(user.getNickName());
            userResponse.setAccessToken(BEARER+accessToken);
            userResponse.setRefreshToken(tokenPair.getToken()); // 실 서비스에서는 보안상 제거

            // cookie 심어주기
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.addHeader(HttpHeaders.SET_COOKIE,
                    refreshCookieFactory.create(tokenPair.getToken(), tokenPair.getExpireIn()).toString());
            objectMapper.writeValue(response.getWriter(), userResponse);

        } catch (AuthenticationException ex) {
            throw new UnauthorizedException(LOGIN_REQUIRED);
        }
    }
}

// Back-End: http://boardapi.sbs.com/1/new

// CORS: white list, Front-End(http://board.sbs.com)
// CSRF
// XSS
