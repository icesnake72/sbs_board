package com.sbs.board.reaction;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.reaction.dto.ReactionRequest;
import com.sbs.board.reaction.dto.ReactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class ReactionController {
    private final ReactionService reactionService;

    @PostMapping("/post/{postId}/reation")
    public ReactionResponse reactToPost(
        @PathVariable Long postId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ReactionRequest request
        ) {
        return reactionService.react(postId, userDetails.getId(), request.type());
    }

    @PostMapping("/comment/{commentId}/reaction")
    public ReactionResponse reactToComment(
        @PathVariable Long commentId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ReactionRequest request
        ) {
        return reactionService.reactToComment(commentId, userDetails.getId(), request.type());
    }

}
