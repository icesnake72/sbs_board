package com.sbs.board.reaction.dto;

import com.sbs.board.reaction.ReactionType;

// 특정 사용자가 여러 대상(게시글/댓글)에 남긴 반응을 한 번에 조회할 때 사용하는 조회 전용 DTO
// (target_id, user_id)에 유니크 제약이 있으므로 대상당 최대 1행이다
public record UserReaction(
        Long targetId,
        ReactionType type
) {
}
