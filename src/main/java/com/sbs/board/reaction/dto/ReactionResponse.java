package com.sbs.board.reaction.dto;

import com.sbs.board.reaction.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionResponse {
    private long likeCount;
    private long dislikeCount;
    private ReactionType myReaction;
}
