package com.sbs.board.auth.oauth;

import com.sbs.board.auth.AuthService;
import com.sbs.board.auth.UserRepository;
import com.sbs.board.auth.dto.UserResponse;
import com.sbs.board.auth.jwt.JwtTokenProvider;
import com.sbs.board.auth.oauth.dto.KakaoTokenResponse;
import com.sbs.board.auth.oauth.dto.KakaoUserResponse;
import com.sbs.board.global.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.sbs.board.auth.jwt.JwtAuthenticationFilter.BEARER;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {
    private static final String KAKAO_PREFIX = "KAKAO_";
    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    public String authorizeUrl(String state) {
        return kakaoOAuthClient.authorizeUrl(state);
    }

    public UserResponse login(String code) {
        KakaoTokenResponse tokenResponse = kakaoOAuthClient.requestToken(code);
        KakaoUserResponse userResponse = kakaoOAuthClient.fetchUserInfo(tokenResponse.getAccessToken());
        System.out.println( userResponse );

        log.debug("Kakao User Email: {}", userResponse.getEmail());

        User user = findOrCreateUser(userResponse);
        String accessToken = jwtTokenProvider.createToken(user.getEmail());
        String refreshToken = authService.issueRefreshToken(user.getId());

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickName(user.getNickName());
        response.setAccessToken(BEARER+accessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }

    private User findOrCreateUser(KakaoUserResponse kakaoUser) {
        String providerId = KAKAO_PREFIX + String.valueOf(kakaoUser.getId());

        return userRepository.findByProviderId(providerId)
                .orElseGet(()->createUser(kakaoUser, providerId));
    }

    private User createUser(KakaoUserResponse kakaoUser, String providerId) {
        String email = kakaoUser.getEmail();
        String password = passwordEncoder.encode(UUID.randomUUID().toString());

        User newUser = new User();
        newUser.setEmail( email );
        newUser.setPassword( password );
        newUser.setNickName( kakaoUser.getNickname() );
        newUser.setProvider("KAKAO");
        newUser.setProviderId(providerId);
        newUser.setProfileImageUrl(kakaoUser.getProfileImageUrl());

        return userRepository.save(newUser);
    }
}
