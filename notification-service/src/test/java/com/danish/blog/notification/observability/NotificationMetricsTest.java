package com.danish.blog.notification.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMetricsTest {

    @Test
    void recordsKafkaOutcomesAndCreatedNotifications() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        NotificationMetrics metrics = new NotificationMetrics(registry);

        metrics.recordConsumed();
        metrics.recordFailure();
        metrics.recordDuplicate();
        metrics.recordRetry();
        metrics.recordDeadLetter();
        metrics.recordNotificationCreated();

        assertKafkaCount(registry, "consumed");
        assertKafkaCount(registry, "failure");
        assertKafkaCount(registry, "duplicate");
        assertKafkaCount(registry, "retry");
        assertKafkaCount(registry, "dead_letter");
        assertThat(registry.counter("blog.notifications.created").count()).isEqualTo(1);
    }

    private void assertKafkaCount(SimpleMeterRegistry registry, String outcome) {
        assertThat(registry.counter("blog.kafka.post.published.events", "outcome", outcome).count()).isEqualTo(1);
    }
}
