package com.sbs.board.auth.oauth2;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.entity.UserProfile;
import com.sbs.board.global.exception.DuplicateException;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.OAuth2DuplicateEmailException;
import com.sbs.board.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    private enum OAuth2Provider {
        KAKAO,
        GOOGLE;
    }

    // OAuth2.0 인증 서버와 토큰 교환이 끝나면 자동으로 사용자 정보 요청을 시도한다.
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        findOrCreate(registrationId, oAuth2User.getAttributes());

        return oAuth2User;
    }

    @Transactional
    public void findOrCreate(String registrationId, Map<String, Object> attributes) {
        // registrationId --> "kakao", "google"

        OAuth2Provider authProvider = OAuth2Provider.valueOf(registrationId.toUpperCase(Locale.ROOT));

        String providerId = (authProvider == OAuth2Provider.KAKAO) ? String.valueOf(attributes.get("id"))
                : String.valueOf(attributes.get("sub"));

        User user = userRepository.findByProviderId(authProvider+"_"+providerId)
            .orElseGet(() -> createUser(authProvider, attributes, providerId));

        userProfileRepository.findByUser(user).orElseGet(
            ()-> {
                UserProfile userProfile = new UserProfile();
                userProfile.setUser( user );
                return userProfileRepository.save(userProfile);
            }
        );
    }

    private void verifySocialEmail(String email, String providerId) {
//        userRepository.findByEmail(email).ifPresent(
        userRepository.findByEmailAndProviderId(email, providerId).ifPresent(
                (user)-> {throw new OAuth2DuplicateEmailException();}
        );
    }


    @Transactional
    private User createUser(OAuth2Provider authProvider, Map<String, Object> attributes, String providerId) {
        // debug
        log.debug("creatUser::authProvider: {}", authProvider);

        String email;
        User newUser = new User();

        String joinedProviderId = authProvider.toString()+"_"+providerId;

        newUser.setProvider(authProvider.toString());
        newUser.setProviderId(joinedProviderId);
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        switch ( authProvider ) {
            case KAKAO -> {
                email = kakaoEmail(attributes);
                verifySocialEmail(email, providerId);

                newUser.setEmail(email);
                newUser.setNickName(kakaoNickName(attributes));
                newUser.setProfileImageUrl(kakaoProfileImage(attributes));
            }
            case GOOGLE -> {
                email = (String) attributes.get("email");
                verifySocialEmail(email, providerId);

                newUser.setEmail(email);
                newUser.setNickName((String) attributes.get("name"));
                newUser.setProfileImageUrl((String) attributes.get("picture"));
            }
        }

        return userRepository.save( newUser );
    }

    private String kakaoNickName(Map<String, Object> attributes) {
        Object account = attributes.get("kakao_account");
        if ( account == null ) {
            return null;
        }

        Map<?,?> accountValue = (Map<?,?>) account;
        Object profileValue = accountValue.get("profile");
        if ( profileValue == null ) {
            return null;
        }

        Map<?,?> profile = (Map<?,?>) profileValue;
        return (String) profile.get("nickname");
    }

    private String kakaoProfileImage(Map<String, Object> attributes) {
        Object account = attributes.get("kakao_account");
        if ( account == null ) {
            return null;
        }

        Map<?,?> accountValue = (Map<?,?>) account;
        Object profileValue = accountValue.get("profile");
        if ( profileValue == null ) {
            return null;
        }

        Map<?,?> profile = (Map<?,?>) profileValue;
        return (String) profile.get("profile_image_url");
    }

    private String kakaoEmail(Map<String, Object> attributes) {
        Object account = attributes.get("kakao_account");
        if ( account == null ) {
            return null;
        }

        Map<?,?> accountValue = (Map<?,?>) account;
        return (String) accountValue.get("email");
    }
}
