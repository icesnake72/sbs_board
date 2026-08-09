package com.sbs.board.comment.dto;

import com.sbs.board.reaction.ReactionType;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String authorUsername,
        String content,
        boolean deleted,
        LocalDateTime createdAt,
        long likeCount,
        long dislikeCount,
        ReactionType myReaction,   // 로그인한 사용자가 남긴 반응, 없으면 null
        List<CommentResponse> children
) {
    public static final String DELETED_CONTENT = "삭제된 게시글입니다";
}
