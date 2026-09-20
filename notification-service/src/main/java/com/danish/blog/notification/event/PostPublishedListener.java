package com.danish.blog.notification.event;

import com.danish.blog.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PostPublishedListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public PostPublishedListener(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.post-published}")
    public void onPostPublished(String payload) {
        try {
            notificationService.consume(objectMapper.readValue(payload, PostPublishedEvent.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid PostPublished event payload", exception);
        }
    }
}
