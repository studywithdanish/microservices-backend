package com.danish.blog.post.service;

import com.danish.blog.post.domain.OutboxEvent;
import com.danish.blog.post.domain.Post;
import com.danish.blog.post.event.PostPublishedEvent;
import com.danish.blog.post.observability.CorrelationIdFilter;
import com.danish.blog.post.observability.OutboxMetrics;
import com.danish.blog.post.observability.OutboxTraceContext;
import com.danish.blog.post.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PostEventOutbox {

    static final String POST_PUBLISHED = "PostPublished";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final OutboxMetrics metrics;
    private final OutboxTraceContext traceContext;

    public PostEventOutbox(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            OutboxMetrics metrics,
            OutboxTraceContext traceContext
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.traceContext = traceContext;
    }

    public void recordPostPublished(Post post) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        PostPublishedEvent event = new PostPublishedEvent(
                eventId,
                1,
                post.getId(),
                post.getAuthorId(),
                post.getTitle(),
                post.getCategoryId(),
                post.getCategoryTitle(),
                occurredAt
        );
        try {
            OutboxTraceContext.TraceHeaders traceHeaders = traceContext.capture();
            outboxEventRepository.save(OutboxEvent.pending(
                    eventId,
                    "Post",
                    post.getId(),
                    POST_PUBLISHED,
                    objectMapper.writeValueAsString(event),
                    CorrelationIdFilter.currentOrCreate(),
                    traceHeaders.traceparent(),
                    traceHeaders.tracestate(),
                    occurredAt
            ));
            metrics.recordCreated();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize PostPublished event", exception);
        }
    }
}
