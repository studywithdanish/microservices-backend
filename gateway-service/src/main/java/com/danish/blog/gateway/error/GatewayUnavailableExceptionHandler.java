package com.danish.blog.gateway.error;

import com.danish.blog.gateway.filter.CorrelationIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GatewayUnavailableExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GatewayUnavailableExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        GatewayFailure failure = classify(exception);
        if (failure == null || exchange.getResponse().isCommitted()) {
            return Mono.error(exception);
        }

        exchange.getResponse().setStatusCode(failure.status());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        if (correlationId != null) {
            exchange.getResponse().getHeaders()
                    .set(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", failure.status().value());
        response.put("error", failure.code());
        response.put("message", failure.message());
        response.put("path", exchange.getRequest().getPath().value());
        response.put("correlationId", correlationId);

        try {
            byte[] body = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException serializationFailure) {
            return Mono.error(serializationFailure);
        }
    }

    private GatewayFailure classify(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (isTimeout(current)) {
                return new GatewayFailure(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "UPSTREAM_TIMEOUT",
                        "The backend service did not respond in time"
                );
            }
            if (current instanceof ConnectException) {
                return new GatewayFailure(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "UPSTREAM_UNAVAILABLE",
                        "The backend service is temporarily unavailable"
                );
            }
            if (current instanceof ResponseStatusException statusException
                    && statusException.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                return new GatewayFailure(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "UPSTREAM_UNAVAILABLE",
                        "The backend service is temporarily unavailable"
                );
            }
            current = current.getCause();
        }
        return null;
    }

    private boolean isTimeout(Throwable exception) {
        return exception instanceof TimeoutException
                || exception.getClass().getSimpleName().endsWith("TimeoutException")
                || (exception instanceof ResponseStatusException statusException
                && statusException.getStatusCode().value() == HttpStatus.GATEWAY_TIMEOUT.value());
    }

    private record GatewayFailure(HttpStatus status, String code, String message) {
    }
}
