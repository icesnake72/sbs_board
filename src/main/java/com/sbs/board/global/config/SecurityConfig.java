package com.sbs.board.global.config;

import com.sbs.board.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 평문을 암호하는 객체, 암호화만 가능
        return new BCryptPasswordEncoder();
    }
    // "abc" - 암호화 알고리즘, 비밀번호(key) -> "00112233"

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)      // CSRF 비활성화
            .formLogin(AbstractHttpConfigurer::disable) // Spring Security Form Login 기능 사용하지 않음
            .httpBasic(AbstractHttpConfigurer::disable) // Spring Security Form Login 기능 사용하지 않음
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 처리
                // 모든 요청을 무조건 허용한다
            .authorizeHttpRequests(auth-> auth.anyRequest().permitAll())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.debug("SecurityConfig에서 SessionCreationPolicy.STATELESS 처리됨");
        log.debug("SecurityConfig에서 jwtAuthenticationFilter추가됨");

        return http.build();
    }
}
