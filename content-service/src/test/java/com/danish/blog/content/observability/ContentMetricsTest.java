package com.danish.blog.content.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentMetricsTest {

    @Test
    void recordsContentChangesWithBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ContentMetrics metrics = new ContentMetrics(registry);

        metrics.recordCommentCreated();
        metrics.recordCommentDeleted();
        metrics.recordCategoryCreated();
        metrics.recordCategoryDeleted();

        assertThat(changeCount(registry, "comment", "created")).isEqualTo(1);
        assertThat(changeCount(registry, "comment", "deleted")).isEqualTo(1);
        assertThat(changeCount(registry, "category", "created")).isEqualTo(1);
        assertThat(changeCount(registry, "category", "deleted")).isEqualTo(1);
    }

    private double changeCount(SimpleMeterRegistry registry, String contentType, String action) {
        return registry.counter("blog.content.changes", "content_type", contentType, "action", action).count();
    }
}
