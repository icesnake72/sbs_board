package com.sbs.board.notification;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.BusinessException;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void create(Long recipientId, Long actorId, NotificationType type, Long postId, Long commentId) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(()->new NotFoundException(ErrorCode.USER_NOT_FOUND));

        User actor = userRepository.findById(actorId)
                .orElseThrow(()->new NotFoundException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .notificationType(type)
                .postId(postId)
                .commentId(commentId)
                .build();

        notificationRepository.save( notification );
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientId(userId, pageable)
                .map(Notification::toResponse);
    }

    @Transactional
    public void read(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(()->new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));

        if ( !notification.isOwnedBy(userId) ) {
            // 권한 에러 발생시킴
            throw new BusinessException(ErrorCode.CANNOT_VIEW_NOTIFICATION);
        }

        notification.markAsRead();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadIsFalse(userId);
    }
}
