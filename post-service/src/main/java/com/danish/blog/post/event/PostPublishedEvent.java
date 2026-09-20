package com.danish.blog.post.event;

import java.time.Instant;
import java.util.UUID;

public record PostPublishedEvent(
        UUID eventId,
        int eventVersion,
        Integer postId,
        Integer authorId,
        String title,
        Integer categoryId,
        String categoryTitle,
        Instant occurredAt
) {
}
