package com.sbs.board.auth;

import com.sbs.board.auth.dto.*;
import com.sbs.board.global.IngestResult;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    public static final String LOGIN_USER_ID = "LoginUserId";

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    @Value("${jwt.refresh-token-validity-seconds}")
    private long refreshTokenExpiresSec;


    @PostMapping("/signup")
    public ResponseEntity<IngestResult> signup(
            @Valid
            @RequestBody
            SignupRequest request) {

        IngestResult result = authService.signUp(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid
            @RequestBody
            LoginRequest request) {

        UserResponse response = authService.login(request);
        ResponseCookie cookie = refreshCookieFactory
                .create(response.getRefreshToken(), refreshTokenExpiresSec);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        if ( refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie cookie = refreshCookieFactory.expire();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/reissue")
    public TokenResponse reissue(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        if ( refreshToken == null) {
            throw new UnauthorizedException(ErrorCode.LOGIN_REQUIRED);
        }

        log.debug("refresh token in cookie: {}", refreshToken);

        return authService.reissueToken(refreshToken);
    }
}

