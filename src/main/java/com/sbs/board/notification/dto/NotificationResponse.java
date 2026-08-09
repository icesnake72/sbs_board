package com.sbs.board.notification.dto;

import com.sbs.board.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String message;
    private String actorUsername;
    private Long postId;
    private Long commentId;
    private boolean read;
    private LocalDateTime createdAt;
}
