package com.danish.blog.post.service;

import com.danish.blog.post.domain.OutboxEvent;
import com.danish.blog.post.domain.Post;
import com.danish.blog.post.event.PostPublishedEvent;
import com.danish.blog.post.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostEventOutboxTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void storesVersionedPostPublishedPayload() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PostEventOutbox outbox = new PostEventOutbox(outboxEventRepository, objectMapper);
        Post post = new Post();
        post.setId(42);
        post.setAuthorId(7);
        post.setTitle("Kafka with Spring");
        post.setCategoryId(3);
        post.setCategoryTitle("Architecture");
        post.setContent("Content");
        post.setImageName("default.png");
        post.setAddedDate(new Date());

        outbox.recordPostPublished(post);

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
    }
}
