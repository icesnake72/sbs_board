package com.sbs.board.reaction.dto;

import com.sbs.board.reaction.ReactionType;
import jakarta.validation.constraints.NotNull;

public record ReactionRequest(@NotNull ReactionType type) {
}
