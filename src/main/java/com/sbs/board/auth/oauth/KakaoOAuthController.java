package com.sbs.board.auth.oauth;

import com.sbs.board.auth.RefreshCookieFactory;
import com.sbs.board.auth.dto.UserResponse;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/oauth/kakao")
public class KakaoOAuthController {
    public static final String STATE_COOKIE_NAME = "oauthState";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private final KakaoOAuthService kakaoOAuthService;
    private final RefreshCookieFactory refreshCookieFactory;
    private final boolean cookieSecure;
    private final long refreshTokenExpiresSec;

    public KakaoOAuthController(
            KakaoOAuthService kakaoOAuthService,
            RefreshCookieFactory refreshCookieFactory,
            @Value("${app.refresh-cookie.secure}") boolean secure,
            @Value("${jwt.refresh-token-validity-seconds}") long expireTokenSec

    ) {
        this.kakaoOAuthService = kakaoOAuthService;
        this.refreshCookieFactory = refreshCookieFactory;
        this.cookieSecure = secure;
        this.refreshTokenExpiresSec = expireTokenSec;
    }


    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        // CSRF - 1회성 code를 생성
        String state = UUID.randomUUID().toString();
        String cookie = stateCookie(state, STATE_TTL).toString();
        log.info("KakoOAuthController.login:cookie: {}", cookie);

        return ResponseEntity.status(HttpStatus.FOUND)  // 302
                .location(URI.create(kakaoOAuthService.authorizeUrl(state)))    // 리다이렉트 주소
                .header(HttpHeaders.SET_COOKIE, cookie)
                .build();
    }

    private ResponseCookie stateCookie(String value, Duration maxAge) {
        return ResponseCookie.from(STATE_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/oauth/kakao")
                .maxAge(maxAge)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<UserResponse> callback(
            @RequestParam("code") String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @CookieValue(name = STATE_COOKIE_NAME, required = false) String stateCookie
    ) {
//        log.info("callback ================================================");
//        log.info("code: {}", code);
//        log.info("state: {}", state);
//        log.info("stateCookie: {}", stateCookie);
//        log.info("================================================");
        if ( error != null || code == null) {
            throw new UnauthorizedException(ErrorCode.LOGIN_FAILED);
        }
        if ( stateCookie == null ) {
            throw new UnauthorizedException(ErrorCode.INVALID_OAUTH_STATE);
        }

        // OAuth login
        UserResponse response = kakaoOAuthService.login(code);

        // 쿠키 설정
        ResponseCookie cookie = refreshCookieFactory
                .create(response.getRefreshToken(), refreshTokenExpiresSec);

        return ResponseEntity.ok().body(response);
    }
}
