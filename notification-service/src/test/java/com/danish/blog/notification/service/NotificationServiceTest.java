package com.danish.blog.notification.service;

import com.danish.blog.notification.domain.Notification;
import com.danish.blog.notification.event.PostPublishedEvent;
import com.danish.blog.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void consumesPostPublishedEventIntoUserNotification() {
        PostPublishedEvent event = event();

        notificationService.consume(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(event.eventId().toString());
        assertThat(captor.getValue().getRecipientUserId()).isEqualTo(7);
        assertThat(captor.getValue().getPostId()).isEqualTo(42);
        assertThat(captor.getValue().getMessage()).contains("Kafka with Spring");
    }

    @Test
    void duplicateEventIsIgnored() {
        PostPublishedEvent event = event();
        when(notificationRepository.existsByEventId(event.eventId().toString())).thenReturn(true);

        notificationService.consume(event);

        verify(notificationRepository, never()).save(any());
    }

    private PostPublishedEvent event() {
        return new PostPublishedEvent(
                UUID.randomUUID(),
                1,
                42,
                7,
                "Kafka with Spring",
                3,
                "Architecture",
                Instant.now()
        );
    }
}
