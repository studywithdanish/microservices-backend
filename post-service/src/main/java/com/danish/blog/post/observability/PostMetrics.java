package com.danish.blog.post.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class PostMetrics {

    private final MeterRegistry registry;
    private final Counter postsCreated;

    public PostMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.postsCreated = Counter.builder("blog.posts.created")
                .description("Successfully created posts")
                .register(registry);
    }

    public void recordPostCreated() {
        postsCreated.increment();
    }

    public <T> T recordImageOperation(String provider, String operation, Supplier<T> operationCall) {
        Timer.Sample sample = Timer.start(registry);
        try {
            T result = operationCall.get();
            recordImageOutcome(sample, provider, operation, "success");
            return result;
        } catch (RuntimeException exception) {
            recordImageOutcome(sample, provider, operation, "failure");
            throw exception;
        }
    }

    public void recordImageOperation(String provider, String operation, Runnable operationCall) {
        recordImageOperation(provider, operation, () -> {
            operationCall.run();
            return null;
        });
    }

    private void recordImageOutcome(Timer.Sample sample, String provider, String operation, String outcome) {
        Counter.builder("blog.images.operations")
                .description("Image storage operations by provider, operation, and outcome")
                .tag("provider", provider)
                .tag("operation", operation)
                .tag("outcome", outcome)
                .register(registry)
                .increment();
        sample.stop(Timer.builder("blog.images.operation.duration")
                .description("Image storage operation duration")
                .tag("provider", provider)
                .tag("operation", operation)
                .tag("outcome", outcome)
                .register(registry));
    }
}
