package com.sbs.board.auth.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Data
@NoArgsConstructor
public class KakaoUserResponse {
    @JsonProperty("id")
    private Long id;    // kakao 회원 id
    private String email;
    private String nickname;
    private String profileImageUrl;

    public KakaoUserResponse(Long id, String email, String nickname, String profileImageUrl) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    // kakao_account unpack
    @JsonProperty("kakao_account")
    private void unpackKakaoAccount(Map<String, Object> kakaoAccount) {
        if ( kakaoAccount == null) {
            return;
        }

        this.email = (String) kakaoAccount.get("email");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        this.nickname = (String) profile.get("nickname");
        this.profileImageUrl = (String) profile.get("profile_image_url");
    }
}
