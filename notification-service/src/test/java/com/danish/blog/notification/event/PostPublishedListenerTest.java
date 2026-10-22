package com.danish.blog.notification.event;

import com.danish.blog.notification.service.NotificationService;
import com.danish.blog.notification.observability.NotificationMetrics;
import com.danish.blog.notification.observability.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class PostPublishedListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationMetrics metrics;

    @Test
    void deserializesVersionedEventAndDelegates() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PostPublishedListener listener = new PostPublishedListener(objectMapper, notificationService, metrics);
        UUID eventId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(new PostPublishedEvent(
                eventId, 1, 42, 7, "Kafka", 3, "Architecture", Instant.now()
        ));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "blog.posts.published.v1",
                0,
                0,
                "42",
                payload
        );
        record.headers().add(
                CorrelationIdFilter.HEADER_NAME,
                "request-kafka-123".getBytes(StandardCharsets.UTF_8)
        );
        doAnswer(invocation -> {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo("request-kafka-123");
            return null;
        }).when(notificationService).consume(any(PostPublishedEvent.class));

        listener.onPostPublished(record);

        ArgumentCaptor<PostPublishedEvent> captor = ArgumentCaptor.forClass(PostPublishedEvent.class);
        verify(notificationService).consume(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(eventId);
        assertThat(captor.getValue().postId()).isEqualTo(42);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenKafkaHeaderIsMissing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PostPublishedListener listener = new PostPublishedListener(objectMapper, notificationService, metrics);
        String payload = objectMapper.writeValueAsString(new PostPublishedEvent(
                UUID.randomUUID(), 1, 42, 7, "Kafka", 3, "Architecture", Instant.now()
        ));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "blog.posts.published.v1",
                0,
                0,
                "42",
                payload
        );
        doAnswer(invocation -> {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY))
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
            return null;
        }).when(notificationService).consume(any(PostPublishedEvent.class));

        listener.onPostPublished(record);

        verify(notificationService).consume(any(PostPublishedEvent.class));
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
