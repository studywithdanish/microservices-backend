package com.danish.blog.post.observability;

import com.danish.blog.post.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxMetrics {

    private final OutboxEventRepository repository;
    private final AtomicLong pendingEvents = new AtomicLong();
    private final AtomicLong failedPendingEvents = new AtomicLong();
    private final Counter recordedEvents;
    private final Counter publishedEvents;
    private final Counter publishFailures;

    public OutboxMetrics(MeterRegistry registry, OutboxEventRepository repository) {
        this.repository = repository;
        this.recordedEvents = eventCounter(registry, "recorded");
        this.publishedEvents = eventCounter(registry, "published");
        this.publishFailures = eventCounter(registry, "publish_failure");
        Gauge.builder("blog.outbox.pending", pendingEvents, AtomicLong::get)
                .description("Unpublished outbox events")
                .register(registry);
        Gauge.builder("blog.outbox.failed.pending", failedPendingEvents, AtomicLong::get)
                .description("Unpublished outbox events with at least one failed attempt")
                .register(registry);
    }

    public void recordCreated() {
        recordedEvents.increment();
        pendingEvents.incrementAndGet();
    }

    public void recordPublished() {
        publishedEvents.increment();
    }

    public void recordPublishFailure() {
        publishFailures.increment();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(
            initialDelayString = "${app.metrics.outbox.initial-delay-ms:15000}",
            fixedDelayString = "${app.metrics.outbox.refresh-interval-ms:15000}"
    )
    public void synchronizeGauges() {
        pendingEvents.set(repository.countByPublishedAtIsNull());
        failedPendingEvents.set(repository.countByPublishedAtIsNullAndAttemptCountGreaterThan(0));
    }

    private Counter eventCounter(MeterRegistry registry, String outcome) {
        return Counter.builder("blog.outbox.events")
                .description("Outbox event lifecycle outcomes")
                .tag("outcome", outcome)
                .register(registry);
    }
}
