package com.sbs.board.auth.oauth;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.oauth.kakao")
public class KakaoOAuthProperties {
    private final String appkey;
    private final String secret;
    private final String callback;
    private final String authorizeUri;
    private final String tokenUri;
    private final String userInfo;

    // 생성자가 호출된 다음 호출됨
    @PostConstruct
    void validate() {
        requireResolve("appkey(KAKAO_REST_API)", appkey);
        requireResolve("secret(KAKAO_SECRET)", secret);
        requireResolve("callback(KAKAO_CALLBACK)", callback);
        requireResolve("authorize-uri", authorizeUri);
        requireResolve("token-uri", tokenUri);
        requireResolve("user-info", userInfo);

        log.info("Kakao REST API Key: {}", appkey);
    }

    private static void requireResolve(String name, String value) {
        if ( value == null || value.isBlank() || value.contains("${")) {
            throw new InternalException("설정값 로딩 실패");
        }
    }
}
