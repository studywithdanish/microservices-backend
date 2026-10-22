package com.danish.blog.post.event;

import com.danish.blog.post.domain.OutboxEvent;
import com.danish.blog.post.repository.OutboxEventRepository;
import com.danish.blog.post.observability.OutboxMetrics;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private OutboxMetrics metrics;

    private OutboxPublisher publisher;
    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(
                outboxEventRepository,
                kafkaTemplate,
                "blog.posts.published.v1",
                1,
                metrics
        );
        event = OutboxEvent.pending(
                UUID.randomUUID(),
                "Post",
                42,
                "PostPublished",
                "{\"postId\":42}",
                "request-outbox-123",
                Instant.now()
        );
        when(outboxEventRepository
                .findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(any()))
                .thenReturn(List.of(event));
    }

    @Test
    void marksEventPublishedAfterKafkaAcknowledgement() {
        CompletableFuture<SendResult<String, String>> result = CompletableFuture.completedFuture(null);
        doReturn(result).when(kafkaTemplate).send(any(ProducerRecord.class));

        publisher.publishPending();

        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttemptCount()).isZero();
        verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, String> record = recordCaptor.getValue();
        assertThat(record.topic()).isEqualTo("blog.posts.published.v1");
        assertThat(record.key()).isEqualTo("42");
        assertThat(record.value()).isEqualTo(event.getPayload());
        assertThat(new String(
                record.headers().lastHeader("X-Correlation-Id").value(),
                StandardCharsets.UTF_8
        )).isEqualTo("request-outbox-123");
        verify(outboxEventRepository).save(event);
    }

    @Test
    void schedulesRetryWhenKafkaPublishFails() {
        CompletableFuture<SendResult<String, String>> result = new CompletableFuture<>();
        result.completeExceptionally(new IllegalStateException("broker unavailable"));
        doReturn(result).when(kafkaTemplate).send(any(ProducerRecord.class));

        publisher.publishPending();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastError()).contains("broker unavailable");
        verify(outboxEventRepository).save(event);
    }
}
