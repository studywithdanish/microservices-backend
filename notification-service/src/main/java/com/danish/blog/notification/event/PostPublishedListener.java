package com.danish.blog.notification.event;

import com.danish.blog.notification.service.NotificationService;
import com.danish.blog.notification.observability.NotificationMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PostPublishedListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationMetrics metrics;

    public PostPublishedListener(
            ObjectMapper objectMapper,
            NotificationService notificationService,
            NotificationMetrics metrics
    ) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.metrics = metrics;
    }

    @KafkaListener(topics = "${app.kafka.topics.post-published}")
    public void onPostPublished(String payload) {
        try {
            notificationService.consume(objectMapper.readValue(payload, PostPublishedEvent.class));
            metrics.recordConsumed();
        } catch (JsonProcessingException exception) {
            metrics.recordFailure();
            throw new IllegalArgumentException("Invalid PostPublished event payload", exception);
        } catch (RuntimeException exception) {
            metrics.recordFailure();
            throw exception;
        }
    }
}
