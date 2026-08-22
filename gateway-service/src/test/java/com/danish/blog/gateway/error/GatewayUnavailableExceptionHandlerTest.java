package com.danish.blog.gateway.error;

import com.danish.blog.gateway.filter.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayUnavailableExceptionHandlerTest {

    private final GatewayUnavailableExceptionHandler handler =
            new GatewayUnavailableExceptionHandler(new ObjectMapper());

    @Test
    void returnsServiceUnavailableForConnectionFailure() {
        MockServerWebExchange exchange = exchange("correlation-123");

        StepVerifier.create(handler.handle(exchange, new ConnectException("Connection refused")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("correlation-123");
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("UPSTREAM_UNAVAILABLE")
                .contains("correlation-123")
                .contains("/api/posts");
    }

    @Test
    void returnsGatewayTimeoutForTimeoutFailure() {
        MockServerWebExchange exchange = exchange("correlation-456");

        StepVerifier.create(handler.handle(exchange, new TimeoutException("Timed out")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("UPSTREAM_TIMEOUT");
    }

    private MockServerWebExchange exchange(String correlationId) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/posts")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                .build());
    }
}
