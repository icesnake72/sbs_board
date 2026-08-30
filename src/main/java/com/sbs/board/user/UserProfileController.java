package com.sbs.board.user;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.auth.LoginUserId;
import com.sbs.board.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public UserProfileResponse me(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return userProfileService.me(userDetails.getId());
    }
}
