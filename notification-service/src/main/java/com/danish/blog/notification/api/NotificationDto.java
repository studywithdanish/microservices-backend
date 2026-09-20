package com.danish.blog.notification.api;

import java.time.Instant;

public record NotificationDto(
        Long id,
        String type,
        String title,
        String message,
        Integer postId,
        boolean read,
        Instant createdAt
) {
}
