package com.sbs.board.notification;

public record CommentCreateEvent(
        Long commentId,
        Long postId,
        Long parentCommentId,
        Long actorId
) {
}
