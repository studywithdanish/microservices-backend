package com.danish.blog.post.observability;

import com.danish.blog.post.repository.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxMetricsTest {

    @Test
    void recordsOutboxLifecycleAndSynchronizesBacklogGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OutboxEventRepository repository = repositoryWithBacklog(7L, 2L);
        OutboxMetrics metrics = new OutboxMetrics(registry, repository);

        metrics.recordCreated();
        metrics.recordPublished();
        metrics.recordPublishFailure();
        metrics.synchronizeGauges();

        assertThat(registry.counter("blog.outbox.events", "outcome", "recorded").count()).isEqualTo(1);
        assertThat(registry.counter("blog.outbox.events", "outcome", "published").count()).isEqualTo(1);
        assertThat(registry.counter("blog.outbox.events", "outcome", "publish_failure").count()).isEqualTo(1);
        assertThat(registry.get("blog.outbox.pending").gauge().value()).isEqualTo(7);
        assertThat(registry.get("blog.outbox.failed.pending").gauge().value()).isEqualTo(2);
    }

    private OutboxEventRepository repositoryWithBacklog(long pending, long failedPending) {
        return (OutboxEventRepository) Proxy.newProxyInstance(
                OutboxEventRepository.class.getClassLoader(),
                new Class<?>[]{OutboxEventRepository.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "countByPublishedAtIsNull" -> pending;
                    case "countByPublishedAtIsNullAndAttemptCountGreaterThan" -> failedPending;
                    default -> null;
                });
    }
}
