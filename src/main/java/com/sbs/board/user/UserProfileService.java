package com.sbs.board.user;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.auth.dto.UserResponse;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.entity.UserProfile;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.global.exception.UnauthorizedException;
import com.sbs.board.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProfileResponse me(Long loginUserId) {
        User user = userRepository.findById(loginUserId)
                .orElseThrow(()->new UnauthorizedException(ErrorCode.LOGIN_REQUIRED));

        UserProfile userProfile = userProfileRepository.findByUser(user)
                .orElseThrow(()->new NotFoundException(ErrorCode.USER_NOT_FOUND));

        return UserProfile.toDTO(userProfile);
    }
}
