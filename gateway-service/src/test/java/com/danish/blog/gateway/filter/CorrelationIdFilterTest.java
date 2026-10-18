package com.danish.blog.gateway.filter;

import com.danish.blog.gateway.config.CorrelationContextConfiguration;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.slf4j.MDC;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    @Test
    void preservesSafeClientCorrelationId() {
        assertThat(CorrelationIdFilter.resolveCorrelationId("request-123.test"))
                .isEqualTo("request-123.test");
    }

    @Test
    void replacesMissingCorrelationId() {
        assertThat(CorrelationIdFilter.resolveCorrelationId(null))
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void replacesUnsafeCorrelationId() {
        assertThat(CorrelationIdFilter.resolveCorrelationId("unsafe\r\nheader"))
                .matches("[0-9a-f-]{36}")
                .isNotEqualTo("unsafe\r\nheader");
    }

    @Test
    void restoresCorrelationIdIntoMdcAcrossReactiveThreadSwitch() {
        new CorrelationContextConfiguration().registerCorrelationMdcAccessor();
        Hooks.enableAutomaticContextPropagation();
        AtomicReference<String> observedMdc = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/posts")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "reactive-request-123")
        );

        new CorrelationIdFilter().filter(
                exchange,
                ignored -> Mono.delay(Duration.ofMillis(10))
                        .doOnNext(value -> observedMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY)))
                        .then()
        ).block(Duration.ofSeconds(2));

        assertThat(observedMdc).hasValue("reactive-request-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
