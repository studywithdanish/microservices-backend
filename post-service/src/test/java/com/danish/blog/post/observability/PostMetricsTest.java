package com.danish.blog.post.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostMetricsTest {

    @Test
    void recordsPostsAndImageOperationOutcomes() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PostMetrics metrics = new PostMetrics(registry);

        metrics.recordPostCreated();
        assertThat(metrics.recordImageOperation("s3", "store", () -> "image-key")).isEqualTo("image-key");
        assertThatThrownBy(() -> metrics.recordImageOperation("s3", "load", () -> {
            throw new IllegalStateException("S3 unavailable");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(registry.counter("blog.posts.created").count()).isEqualTo(1);
        assertThat(registry.counter("blog.images.operations", "provider", "s3", "operation", "store", "outcome", "success").count()).isEqualTo(1);
        assertThat(registry.counter("blog.images.operations", "provider", "s3", "operation", "load", "outcome", "failure").count()).isEqualTo(1);
        assertThat(registry.timer("blog.images.operation.duration", "provider", "s3", "operation", "store", "outcome", "success").count()).isEqualTo(1);
        assertThat(registry.timer("blog.images.operation.duration", "provider", "s3", "operation", "load", "outcome", "failure").count()).isEqualTo(1);
    }
}
