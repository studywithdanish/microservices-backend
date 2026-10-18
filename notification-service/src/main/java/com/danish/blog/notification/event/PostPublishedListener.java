package com.danish.blog.notification.event;

import com.danish.blog.notification.observability.CorrelationIdFilter;
import com.danish.blog.notification.observability.NotificationMetrics;
import com.danish.blog.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

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
    public void onPostPublished(ConsumerRecord<String, String> record) {
        String correlationId = correlationId(record);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(
                CorrelationIdFilter.MDC_KEY,
                correlationId
        )) {
            try {
                notificationService.consume(
                        objectMapper.readValue(record.value(), PostPublishedEvent.class)
                );
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

    private String correlationId(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(CorrelationIdFilter.HEADER_NAME);
        String candidate = header == null || header.value() == null
                ? null
                : new String(header.value(), StandardCharsets.UTF_8);
        return CorrelationIdFilter.resolve(candidate);
    }
}
