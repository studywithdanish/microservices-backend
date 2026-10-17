package com.danish.blog.notification.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final Counter consumedEvents;
    private final Counter failedEvents;
    private final Counter duplicateEvents;
    private final Counter retryAttempts;
    private final Counter deadLetterEvents;
    private final Counter notificationsCreated;

    public NotificationMetrics(MeterRegistry registry) {
        consumedEvents = kafkaCounter(registry, "consumed");
        failedEvents = kafkaCounter(registry, "failure");
        duplicateEvents = kafkaCounter(registry, "duplicate");
        retryAttempts = kafkaCounter(registry, "retry");
        deadLetterEvents = kafkaCounter(registry, "dead_letter");
        notificationsCreated = Counter.builder("blog.notifications.created")
                .description("Notifications created from domain events")
                .register(registry);
    }

    public void recordConsumed() {
        consumedEvents.increment();
    }

    public void recordFailure() {
        failedEvents.increment();
    }

    public void recordDuplicate() {
        duplicateEvents.increment();
    }

    public void recordRetry() {
        retryAttempts.increment();
    }

    public void recordDeadLetter() {
        deadLetterEvents.increment();
    }

    public void recordNotificationCreated() {
        notificationsCreated.increment();
    }

    private Counter kafkaCounter(MeterRegistry registry, String outcome) {
        return Counter.builder("blog.kafka.post.published.events")
                .description("PostPublished Kafka event outcomes")
                .tag("outcome", outcome)
                .register(registry);
    }
}
