package com.danish.blog.observability;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationPropagationConfigurationTest {

    private static final Path REPOSITORY_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void everyActiveHttpApplicationOwnsCorrelationContext() throws IOException {
        assertSourceContains(
                "gateway-service/src/main/java/com/danish/blog/gateway/filter/CorrelationIdFilter.java",
                "contextWrite",
                "X-Correlation-Id"
        );
        assertSourceContains(
                "gateway-service/src/main/resources/application.yml",
                "context-propagation: auto"
        );
        for (String service : new String[]{"identity", "post", "content", "notification"}) {
            assertThat(Files.exists(REPOSITORY_ROOT.resolve(
                    service + "-service/src/main/java/com/danish/blog/" + service
                            + "/observability/CorrelationIdFilter.java"
            ))).isTrue();
        }
    }

    @Test
    void privateHttpCallsForwardCorrelationHeader() throws IOException {
        assertSourceContains(
                "post-service/src/main/java/com/danish/blog/post/client/CategoryClient.java",
                "CorrelationIdFilter.HEADER_NAME",
                "CorrelationIdFilter.currentOrCreate()"
        );
        assertSourceContains(
                "content-service/src/main/java/com/danish/blog/content/client/HttpPostReferenceClient.java",
                "CorrelationIdFilter.HEADER_NAME",
                "CorrelationIdFilter.currentOrCreate()"
        );
    }

    @Test
    void outboxPersistsCorrelationBeforeKafkaPublication() throws IOException {
        assertSourceContains(
                "post-service/src/main/java/com/danish/blog/post/domain/OutboxEvent.java",
                "correlationId"
        );
        assertSourceContains(
                "post-service/src/main/java/com/danish/blog/post/event/OutboxPublisher.java",
                "ProducerRecord",
                "CorrelationIdFilter.HEADER_NAME"
        );
        assertSourceContains(
                "notification-service/src/main/java/com/danish/blog/notification/event/PostPublishedListener.java",
                "ConsumerRecord",
                "CorrelationIdFilter.HEADER_NAME"
        );
        assertSourceContains(
                "post-service/src/main/resources/db/migration/V3__add_correlation_id_to_post_outbox.sql",
                "correlation_id",
                "not null"
        );
    }

    private void assertSourceContains(String relativePath, String... expectedValues) throws IOException {
        String source = Files.readString(REPOSITORY_ROOT.resolve(relativePath));
        assertThat(source).contains(expectedValues);
    }
}
