package com.sbs.board.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // actor도 join으로 함께 로딩
    @EntityGraph(attributePaths = {"actor"})
    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadIsFalse(Long recipientId);
}
