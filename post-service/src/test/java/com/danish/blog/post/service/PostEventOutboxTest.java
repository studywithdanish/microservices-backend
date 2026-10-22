package com.danish.blog.post.service;

import com.danish.blog.post.domain.OutboxEvent;
import com.danish.blog.post.domain.Post;
import com.danish.blog.post.event.PostPublishedEvent;
import com.danish.blog.post.observability.OutboxMetrics;
import com.danish.blog.post.observability.CorrelationIdFilter;
import com.danish.blog.post.observability.OutboxTraceContext;
import com.danish.blog.post.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostEventOutboxTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxMetrics metrics;

    @Mock
    private OutboxTraceContext traceContext;

    @Test
    void storesVersionedPostPublishedPayload() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PostEventOutbox outbox = new PostEventOutbox(
                outboxEventRepository,
                objectMapper,
                metrics,
                traceContext
        );
        Post post = new Post();
        post.setId(42);
        post.setAuthorId(7);
        post.setTitle("Kafka with Spring");
        post.setCategoryId(3);
        post.setCategoryTitle("Architecture");
        post.setContent("Content");
        post.setImageName("default.png");
        post.setAddedDate(new Date());

        when(traceContext.capture()).thenReturn(new OutboxTraceContext.TraceHeaders(
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                "vendor=value"
        ));
        MDC.put(CorrelationIdFilter.MDC_KEY, "request-publish-123");
        try {
            outbox.recordPostPublished(post);
        } finally {
            MDC.clear();
        }

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        PostPublishedEvent event = objectMapper.readValue(
                captor.getValue().getPayload(),
                PostPublishedEvent.class
        );
        assertThat(event.eventId()).isEqualTo(captor.getValue().getEventId());
        assertThat(event.eventVersion()).isEqualTo(1);
        assertThat(event.postId()).isEqualTo(42);
        assertThat(event.authorId()).isEqualTo(7);
        assertThat(captor.getValue().getCorrelationId()).isEqualTo("request-publish-123");
        assertThat(captor.getValue().getTraceparent())
                .isEqualTo("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        assertThat(captor.getValue().getTracestate()).isEqualTo("vendor=value");
    }
}
