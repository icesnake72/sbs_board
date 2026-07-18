package com.sbs.board.auth.oauth;

import com.sbs.board.auth.oauth.dto.KakaoTokenResponse;
import com.sbs.board.auth.oauth.dto.KakaoUserResponse;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {
    private final KakaoOAuthProperties properties;
    private final RestClient restClient;

    public String authorizeUrl(String state) {
        String uri = UriComponentsBuilder.fromUriString(properties.getAuthorizeUri())
                .queryParam("client_id", properties.getAppkey())
                .queryParam("redirect_uri", properties.getCallback())
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .toUriString();

        log.info("카카오 인증 URI: {}", uri);
        return uri;
    }

    public KakaoTokenResponse requestToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getAppkey());
        form.add("client_secret", properties.getSecret());
        form.add("redirect_uri", properties.getCallback());
        form.add("code", code);

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                // log 출력하여 에러 내용 확인하기
                throw new UnauthorizedException(ErrorCode.LOGIN_FAILED);
            }

            return response;
        } catch (RestClientException ex) {
            // log 출력하여 에러 내용 확인하기
            throw new UnauthorizedException(ErrorCode.LOGIN_FAILED);
        }

    }

    public KakaoUserResponse fetchUserInfo(String accessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(properties.getUserInfo())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if ( response == null || response.getId() == null) {
                throw new UnauthorizedException(ErrorCode.LOGIN_FAILED);
            }

            return response;
        } catch (RestClientException ex) {
            throw new UnauthorizedException(ErrorCode.LOGIN_FAILED);
        }
    }
}
