package com.danish.blog.notification.repository;

import com.danish.blog.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventId(String eventId);

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Integer recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Integer recipientUserId);
}
