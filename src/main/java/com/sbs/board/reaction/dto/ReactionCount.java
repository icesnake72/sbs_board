package com.sbs.board.reaction.dto;

import com.sbs.board.reaction.ReactionType;

// 목록의 반응 개수를 한 번에 집계할 때 사용하는 조회 전용 DTO
// targetId는 집계 대상의 id(게시글이면 post_id, 댓글이면 comment_id)
// target + type 조합당 한 행이 만들어진다 (대상 하나에 최대 2행: LIKE, DISLIKE)
public record ReactionCount(
        Long targetId,
        ReactionType type,
        long count
) {
}
