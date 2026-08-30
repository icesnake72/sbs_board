package com.sbs.board.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long accessTokenValiditySeconds;

    // 생성자
    public JwtTokenProvider(
        @Value("${jwt.secret}") String base64Secret,
        @Value("${jwt.access-token-validity-seconds}") long accessTokenSeconds
    ) {
        // Base64 시크릿을 바이트로 디코드하여 서명용 Secret Key를 생성함(HS256알고리즘, 최소 32바이트)
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessTokenValiditySeconds = accessTokenSeconds;
        log.debug("JwtTokenProvider 생성됨: {}", base64Secret);
    }

    // userId를 subject에 담고 만료시간을 정해 서명된 토큰 문자열을 생성한다
    public String createToken(String userEmail) {
        Date now = new Date();  // issued at(발급시간)
        Date expiration = new Date(now.getTime() + accessTokenValiditySeconds * 1000); // 만료시간
        return Jwts.builder()
                .subject(userEmail)
                .id(UUID.randomUUID().toString())   // jti: 토큰의 고유 식별자 -> deny list의 키
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 서명된 토큰 문자열을 받아 userId를 반환한다.
    public String getUserEmail(String token) {
        String subject = parseClaims(token).getSubject();

        log.debug("토큰으로부터 사용자 ID 추출: {}", subject);

        return subject;
    }

    public String getJti(String token) {
        return parseClaims(token).getId();
    }

    public long getRemainingSeconds(String token) {
        // 토큰의 남은 유효 시간 구하기
        long remainMillis = parseClaims(token).getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(1, remainMillis/1000);  // 최소 1초
    }


    // 서명된 토큰을 검증한다
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            log.debug("유효한 토큰: {}", token);
            return true;
        } catch (Exception e) {
            log.debug("올바르지 않은 토큰: {}", token);
            return false;
        }
    }
}
