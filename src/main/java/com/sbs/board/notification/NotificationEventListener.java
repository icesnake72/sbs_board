package com.sbs.board.notification;

import com.sbs.board.comment.CommentRepository;
import com.sbs.board.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommentCreated(CommentCreateEvent event) {
        log.debug("댓글 생성 이벤트가 발생됨!!!");
        NotificationType type;
        Long recipientId = null;
        if ( event.parentCommentId() == null ) {
            // 게시글에 대한 댓글 알림 이벤트
            recipientId = postRepository.findAuthorIdById(event.postId()).orElse(null);
            type = NotificationType.COMMENT_ON_POST;
        } else {
            // 댓글에 대한 대댓글 알림 이벤트
            recipientId = commentRepository.findAuthorIdById(event.parentCommentId()).orElse(null);
            type = NotificationType.REPLY_ON_COMMENT;
        }

        if ( recipientId == null || recipientId.equals(event.actorId())) {
            log.error("이벤트를 받을 대상이 없거나, 이벤트 리스너와 발생자가 같습니다");
            return;
        }

        try {
            // Notification 생성 및 저장 작업
            notificationService.create(
                    recipientId,
                    event.actorId(),
                    type,
                    event.postId(),
                    event.commentId());
        } catch (RuntimeException ex) {
            log.error("알림 저장중 에러 발생: {}", ex.getMessage());
        }
    }
}
