package com.danish.blog.post.observability;

import com.danish.blog.post.domain.OutboxEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores W3C trace context beside an outbox event and restores it when the
 * delayed Kafka publication runs. This preserves the original request trace
 * across the database-backed asynchronous boundary without coupling the
 * business code to a tracing backend.
 */
@Component
public class OutboxTraceContext {

    private static final String TRACEPARENT = "traceparent";
    private static final String TRACESTATE = "tracestate";

    private final Tracer tracer;
    private final Propagator propagator;

    @Autowired
    public OutboxTraceContext(
            ObjectProvider<Tracer> tracerProvider,
            ObjectProvider<Propagator> propagatorProvider
    ) {
        this(
                tracerProvider.getIfAvailable(() -> Tracer.NOOP),
                propagatorProvider.getIfAvailable(() -> Propagator.NOOP)
        );
    }

    public OutboxTraceContext(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public TraceHeaders capture() {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return TraceHeaders.empty();
        }

        Map<String, String> carrier = new LinkedHashMap<>();
        propagator.inject(currentSpan.context(), carrier, Map::put);
        return new TraceHeaders(header(carrier, TRACEPARENT), header(carrier, TRACESTATE));
    }

    public PublishScope openPublishScope(OutboxEvent event, String topic) {
        Map<String, String> carrier = new LinkedHashMap<>();
        putIfPresent(carrier, TRACEPARENT, event.getTraceparent());
        putIfPresent(carrier, TRACESTATE, event.getTracestate());

        Span.Builder spanBuilder = carrier.isEmpty()
                ? tracer.spanBuilder()
                : propagator.extract(carrier, Map::get);
        Span span = spanBuilder
                .name("post.outbox.publish")
                .tag("messaging.system", "kafka")
                .tag("messaging.destination.name", topic)
                .tag("messaging.message.id", event.getEventId().toString())
                .start();
        return new PublishScope(span, tracer.withSpan(span));
    }

    private static String header(Map<String, String> carrier, String expectedName) {
        return carrier.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(expectedName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static void putIfPresent(Map<String, String> carrier, String name, String value) {
        if (value != null && !value.isBlank()) {
            carrier.put(name, value);
        }
    }

    public record TraceHeaders(String traceparent, String tracestate) {
        static TraceHeaders empty() {
            return new TraceHeaders(null, null);
        }
    }

    public static final class PublishScope implements AutoCloseable {
        private final Span span;
        private final Tracer.SpanInScope scope;

        private PublishScope(Span span, Tracer.SpanInScope scope) {
            this.span = span;
            this.scope = scope;
        }

        public void error(Throwable throwable) {
            span.error(throwable);
        }

        @Override
        public void close() {
            scope.close();
            span.end();
        }
    }
}
