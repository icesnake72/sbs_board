package com.sbs.board.auth;

import com.sbs.board.auth.dto.LoginRequest;
import com.sbs.board.auth.dto.SignupRequest;
import com.sbs.board.auth.dto.TokenResponse;
import com.sbs.board.auth.dto.UserResponse;
import com.sbs.board.auth.jwt.JwtTokenProvider;
import com.sbs.board.global.entity.RefreshToken;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.entity.UserProfile;
import com.sbs.board.global.IngestResult;
import com.sbs.board.global.exception.DuplicateException;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.global.exception.UnauthorizedException;
import com.sbs.board.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.sbs.board.auth.jwt.JwtAuthenticationFilter.BEARER;
import static com.sbs.board.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.access-token-validity-seconds}")
    private long accessTokenValiditySeconds;

    @Value("${jwt.refresh-token-validity-seconds}")
    private long refreshTokenValiditySeconds;

    @Transactional
    public IngestResult signUp(SignupRequest request) {
        // signup 성공이면 IngestResult 인스턴스에 status = "ok", message = "";
        // signup 실패하면 IngestResult 인스턴스에 status = "error", message = "실패 사유";
        IngestResult result = new IngestResult();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateException(DUPLICATE_USER_EMAIL);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickName(request.getNickName());
        user.setRole(request.getRole().equals("ADMIN") ? User.Role.ADMIN : User.Role.USER);

        User savedUser = userRepository.save(user);

        if (!userProfileRepository.existsByUser(savedUser)) {
            // 저장된 사용자 인증정보와 매칭되는 프로필 정보도 같이 저장한다.
            UserProfile profile = new UserProfile();
            profile.setUser(savedUser);
            userProfileRepository.save( profile );
        }

        result.setStatus("ok");
        return result;
    }

    @Transactional
    public UserResponse login(LoginRequest request) {
        UserResponse response = new UserResponse();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.createToken(userDetails.getUsername());
            String refreshToken = issueRefreshToken(userDetails.getId());

            response.setId(userDetails.getId());
            response.setEmail(userDetails.getUsername());
            response.setNickName(userDetails.getNickName());
//            response.setRole(userDetails.getAuthorities());
            response.setAccessToken(BEARER+accessToken);
            response.setRefreshToken(refreshToken);

        } catch (AuthenticationException ex) {
            throw new UnauthorizedException(LOGIN_REQUIRED);
        }

//        // request로부터 주어진 email로 데이터베이스에서 쿼리하여 User Entity를 가져온다.
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(()-> new NotFoundException(USER_NOT_FOUND));
//
//        // 패스워드 매칭
//        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
//            // 비밀번호가 일치하지 않음
//            throw new UnauthorizedException(LOGIN_FAILED);
//        }

//        String accessToken = jwtTokenProvider.createToken(user.getEmail());

        return response;
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
            .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public String issueRefreshToken(Long userId) {
        String token = UUID.randomUUID().toString();

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenValiditySeconds);
        refreshTokenRepository.findByUserId(userId)
            .ifPresentOrElse(
                exist -> exist.update(token, expiresAt),
                ()-> refreshTokenRepository.save(new RefreshToken(userId, token, expiresAt)));

        return token;
    }

    @Transactional
    public TokenPair issueRefreshTokenPair(Long userId) {
        String token = UUID.randomUUID().toString();

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenValiditySeconds);
        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        exist -> exist.update(token, expiresAt),
                        ()-> refreshTokenRepository.save(new RefreshToken(userId, token, expiresAt)));

        TokenPair tokenPair = new TokenPair();
        tokenPair.setToken(token);
        tokenPair.setExpireIn(refreshTokenValiditySeconds);

        return tokenPair;
    }

    @Transactional
    public TokenResponse reissueToken(String refreshToken) {
        RefreshToken saved = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(()->new UnauthorizedException(LOGIN_REQUIRED));

        if (saved.isExpired()) {
            refreshTokenRepository.deleteByToken(refreshToken);
            throw new UnauthorizedException(LOGIN_REQUIRED);
        }

        User user = userRepository.findById(saved.getUserId())
            .orElseThrow(()-> new UnauthorizedException(LOGIN_REQUIRED));

        String newAccessToken = jwtTokenProvider.createToken(user.getEmail());

        TokenResponse response = new TokenResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }
}
