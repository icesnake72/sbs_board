package com.sbs.board.auth.oauth2;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbs.board.global.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

@Slf4j
@Component
public class CustomOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String COOKIE_NAME = "oauthRequest";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final ObjectMapper objectMapper;    // json data 생성기
    private final boolean cookieSecure;

    record StoredRequest(
            String state,   // 위조 방지 인증 정보
            String authorizationUri,
            String clientId,
            String redirectUri,
            Set<String> scopes,
            String registrationId
    ) {}

    public CustomOAuth2AuthorizationRequestRepository(
            ObjectMapper objectMapper,
            @Value("${app.refresh-cookie.secure}") boolean secure
    ) {
        this.objectMapper = objectMapper;
        this.cookieSecure = secure;
    }

    // 쿠키 객체 생성 메서드
    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    // 쿠키 제거
    private void expireCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }


    // http://localhost:8090/oauth2/authorization/kakao
    //
    // 위조 방지 코드를 만들어 request에 cookie 형태로 실어 보낸다 --> <인증 요청 객체>
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        //
        if ( authorizationRequest == null) {
            return;
        }

        StoredRequest store = new StoredRequest(
                authorizationRequest.getState(),    // 위변조 방지 코드
                authorizationRequest.getAuthorizationUri(),
                authorizationRequest.getClientId(),
                authorizationRequest.getRedirectUri(),
                authorizationRequest.getScopes(),
                authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID));
        try {
            String json = objectMapper.writeValueAsString(store);
            String value = Base64.getUrlEncoder().encodeToString(
                    json.getBytes(StandardCharsets.UTF_8));

            response.addHeader(HttpHeaders.SET_COOKIE, cookie(value, TTL).toString());
        } catch (Exception e) {
            throw new RuntimeException("인증 요청 쿠키 직렬화 실패");
        }
    }


    // 쿠키를 복원하여 state값을 대조한다(위변조 방지 검증)
    // 검증은 Spring Security 의 라이브러리에서 진행한다.
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = findCookie(request);
        if ( cookie == null ) {
            return null;
        }

        try {
            String json = new String(
                    Base64.getUrlDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8
            );

            // 쿠키에 저장한 StoredRequest 객체 복원
            StoredRequest store = objectMapper.readValue(json, StoredRequest.class);

            // debug
            log.debug("loadAuthorizationRequest()::redirect-uri: {}", store.redirectUri());

            return OAuth2AuthorizationRequest.authorizationCode()
                    .state(store.state())
                    .authorizationUri(store.authorizationUri())
                    .clientId(store.clientId())
                    .redirectUri(store.redirectUri())
                    .scopes(store.scopes())
                    .attributes( attrs ->
                            attrs.put(OAuth2ParameterNames.REGISTRATION_ID, store.registrationId()) )
                    .build();
        } catch (Exception e) {
            log.warn("쿠키 복원 실패: {}", e.getMessage());
            return null;
        }
    }

    // 이름이 COOKIE_NAME 인 쿠키 객체 찾기
    private Cookie findCookie(HttpServletRequest request) {
        if ( request.getCookies() == null ) {
            return null;
        }

        for(Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }


    // callback 시 호출되는 메서드
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest auth2AuthorizationRequest = loadAuthorizationRequest(request);
        expireCookie(response);

        return auth2AuthorizationRequest;
    }
}
