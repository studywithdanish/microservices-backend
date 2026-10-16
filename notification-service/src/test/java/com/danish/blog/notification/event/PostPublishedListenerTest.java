package com.danish.blog.notification.event;

import com.danish.blog.notification.service.NotificationService;
import com.danish.blog.notification.observability.NotificationMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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

        listener.onPostPublished(payload);

        ArgumentCaptor<PostPublishedEvent> captor = ArgumentCaptor.forClass(PostPublishedEvent.class);
        verify(notificationService).consume(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(eventId);
        assertThat(captor.getValue().postId()).isEqualTo(42);
    }
}
