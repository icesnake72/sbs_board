package com.sbs.board.user;

import com.sbs.board.auth.dto.UserResponse;
import com.sbs.board.global.entity.UserProfile;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.UnauthorizedException;
import com.sbs.board.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;

    public UserProfileResponse me(Long loginUserId) {
        UserProfile user = userProfileRepository.findById(loginUserId)
                .orElseThrow(()->new UnauthorizedException(ErrorCode.LOGIN_REQUIRED));

        return UserProfile.toDTO(user);
    }
}
