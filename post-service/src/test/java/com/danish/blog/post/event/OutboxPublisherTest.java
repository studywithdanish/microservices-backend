package com.danish.blog.post.event;

import com.danish.blog.post.domain.OutboxEvent;
import com.danish.blog.post.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher publisher;
    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(
                outboxEventRepository,
                kafkaTemplate,
                "blog.posts.published.v1",
                1
        );
        event = OutboxEvent.pending(
                UUID.randomUUID(),
                "Post",
                42,
                "PostPublished",
                "{\"postId\":42}",
                Instant.now()
        );
        when(outboxEventRepository
                .findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(any()))
                .thenReturn(List.of(event));
    }

    @Test
    void marksEventPublishedAfterKafkaAcknowledgement() {
        CompletableFuture<SendResult<String, String>> result = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send("blog.posts.published.v1", "42", event.getPayload())).thenReturn(result);

        publisher.publishPending();

        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttemptCount()).isZero();
        verify(outboxEventRepository).save(event);
    }

    @Test
    void schedulesRetryWhenKafkaPublishFails() {
        CompletableFuture<SendResult<String, String>> result = new CompletableFuture<>();
        result.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafkaTemplate.send("blog.posts.published.v1", "42", event.getPayload())).thenReturn(result);

        publisher.publishPending();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastError()).contains("broker unavailable");
        verify(outboxEventRepository).save(event);
    }
}
