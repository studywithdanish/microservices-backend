package com.danish.blog.post.event;

import com.danish.blog.post.domain.OutboxEvent;
import com.danish.blog.post.observability.CorrelationIdFilter;
import com.danish.blog.post.observability.OutboxMetrics;
import com.danish.blog.post.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.kafka.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final Duration publishTimeout;
    private final OutboxMetrics metrics;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.topics.post-published}") String topic,
            @Value("${app.kafka.outbox.publish-timeout-seconds:10}") long publishTimeoutSeconds,
            OutboxMetrics metrics
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.publishTimeout = Duration.ofSeconds(publishTimeoutSeconds);
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        for (OutboxEvent event : outboxEventRepository
                .findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(Instant.now())) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                topic,
                event.getAggregateId().toString(),
                event.getPayload()
        );
        record.headers().add(
                CorrelationIdFilter.HEADER_NAME,
                event.getCorrelationId().getBytes(StandardCharsets.UTF_8)
        );

        try (MDC.MDCCloseable ignored = MDC.putCloseable(
                CorrelationIdFilter.MDC_KEY,
                event.getCorrelationId()
        )) {
            kafkaTemplate.send(record)
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);
            event.markPublished(Instant.now());
            outboxEventRepository.save(event);
            metrics.recordPublished();
            logger.info("Published outbox event {} to topic {}", event.getEventId(), topic);
        } catch (Exception exception) {
            event.markFailed(exception.getMessage(), Instant.now());
            outboxEventRepository.save(event);
            metrics.recordPublishFailure();
            logger.warn(
                    "Kafka publish failed for event {} on attempt {}: {}",
                    event.getEventId(),
                    event.getAttemptCount(),
                    exception.getMessage()
            );
        }
    }
}
