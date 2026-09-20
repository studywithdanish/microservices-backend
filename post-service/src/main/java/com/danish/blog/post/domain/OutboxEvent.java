package com.danish.blog.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "post_outbox_events")
public class OutboxEvent {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "event_id", nullable = false, length = 36)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Integer aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected OutboxEvent() {
    }

    private OutboxEvent(
            UUID eventId,
            String aggregateType,
            Integer aggregateId,
            String eventType,
            String payload,
            Instant occurredAt
    ) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.nextAttemptAt = occurredAt;
    }

    public static OutboxEvent pending(
            UUID eventId,
            String aggregateType,
            Integer aggregateId,
            String eventType,
            String payload,
            Instant occurredAt
    ) {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload, occurredAt);
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String error, Instant failedAt) {
        attemptCount++;
        long delaySeconds = Math.min(60L, 1L << Math.min(attemptCount, 6));
        nextAttemptAt = failedAt.plus(delaySeconds, ChronoUnit.SECONDS);
        lastError = error == null ? "Kafka publish failed" : error.substring(0, Math.min(error.length(), 1000));
    }

    public UUID getEventId() {
        return eventId;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }
}
