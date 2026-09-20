package com.danish.blog.notification.service;

import com.danish.blog.notification.api.NotificationDto;
import com.danish.blog.notification.domain.Notification;
import com.danish.blog.notification.error.ResourceNotFoundException;
import com.danish.blog.notification.event.PostPublishedEvent;
import com.danish.blog.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void consume(PostPublishedEvent event) {
        validate(event);
        String eventId = event.eventId().toString();
        if (notificationRepository.existsByEventId(eventId)) {
            return;
        }
        notificationRepository.save(new Notification(
                eventId,
                event.authorId(),
                event.postId(),
                "POST_PUBLISHED",
                "Post published",
                "Your post '" + event.title() + "' was published successfully.",
                event.occurredAt()
        ));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getForUser(Integer userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public NotificationDto markRead(Long notificationId, Integer userId) {
        Notification notification = notificationRepository
                .findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.markRead();
        return toDto(notificationRepository.save(notification));
    }

    private void validate(PostPublishedEvent event) {
        if (event == null
                || event.eventId() == null
                || event.eventVersion() != 1
                || event.postId() == null
                || event.authorId() == null
                || event.title() == null
                || event.title().isBlank()
                || event.occurredAt() == null) {
            throw new IllegalArgumentException("Unsupported or incomplete PostPublished event");
        }
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getPostId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
