package com.danish.blog.content.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ContentMetrics {

    private final Counter commentsCreated;
    private final Counter commentsDeleted;
    private final Counter categoriesCreated;
    private final Counter categoriesDeleted;

    public ContentMetrics(MeterRegistry registry) {
        commentsCreated = changeCounter(registry, "comment", "created");
        commentsDeleted = changeCounter(registry, "comment", "deleted");
        categoriesCreated = changeCounter(registry, "category", "created");
        categoriesDeleted = changeCounter(registry, "category", "deleted");
    }

    public void recordCommentCreated() {
        commentsCreated.increment();
    }

    public void recordCommentDeleted() {
        commentsDeleted.increment();
    }

    public void recordCategoryCreated() {
        categoriesCreated.increment();
    }

    public void recordCategoryDeleted() {
        categoriesDeleted.increment();
    }

    private Counter changeCounter(MeterRegistry registry, String contentType, String action) {
        return Counter.builder("blog.content.changes")
                .description("Successful content changes")
                .tag("content_type", contentType)
                .tag("action", action)
                .register(registry);
    }
}
