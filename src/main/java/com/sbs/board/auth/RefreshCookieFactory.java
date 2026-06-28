package com.sbs.board.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieFactory {
    private final String name;
    private final boolean secure;
    private final String sameSite;
    private final String path;

    public RefreshCookieFactory(
        @Value("${app.refresh-cookie.name}") String name,
        @Value("${app.refresh-cookie.secure}") boolean secure,
        @Value("${app.refresh-cookie.same-site}") String sameSite,
        @Value("${app.refresh-cookie.path}") String path
    ) {
        this.name = name;
        this.secure = secure;
        this.sameSite = sameSite;
        this.path = path;
    }

    public String cookieName() {
        return name;
    }

    // 쿠키 발급(생성)
    public ResponseCookie create(String refreshToken, long maxAgeSec) {
        return ResponseCookie.from(name, refreshToken)
                .httpOnly(true) // http로만 전송된다
                .secure(secure) // 보안여부(secure channel을 사용?)
                .sameSite(sameSite) //
                .path(path)     // 지정된 라우트에만 전송
                .maxAge(maxAgeSec)  // 쿠키의 만료시간
                .build();
    }


    // 삭제용 : 이런식으로 쿠키를 발급하여 즉시 만료(무효화)
    public ResponseCookie expire() {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0)
                .build();
    }
}
