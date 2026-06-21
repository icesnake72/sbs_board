package com.sbs.board.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.sbs.board.auth.AuthController.LOGIN_USER_ID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String BEARER = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // request로부터 토큰 추출
        String token = resolveToken(request);
        if (token!=null && jwtTokenProvider.validateToken(token)) {
            request.setAttribute(LOGIN_USER_ID, jwtTokenProvider.getUserId(token));
            log.debug("JwtAuthenticationFilter::doFilterInternal: {}: {}", LOGIN_USER_ID, request.getAttribute(LOGIN_USER_ID));
        }

        // 다음 필터에 request, response 객체를 전달한다
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER)) {
            return header.substring(BEARER.length());
        }

        return null;
    }
}
