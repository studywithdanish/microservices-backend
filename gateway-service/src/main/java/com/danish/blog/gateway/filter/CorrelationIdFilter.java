package com.danish.blog.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final Pattern VALID_CORRELATION_ID =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$");
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(
                exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER)
        );
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(CORRELATION_ID_HEADER, correlationId))
                .build();
        ServerWebExchange filteredExchange = exchange.mutate().request(request).build();
        long startedAt = System.nanoTime();

        filteredExchange.getResponse().beforeCommit(() -> {
            filteredExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);
            return Mono.empty();
        });
        return chain.filter(filteredExchange)
                .doFinally(signalType -> logApplicationRequest(filteredExchange, startedAt))
                .contextWrite(context -> context.put(MDC_KEY, correlationId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    static String resolveCorrelationId(String candidate) {
        if (candidate != null && VALID_CORRELATION_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private void logApplicationRequest(ServerWebExchange exchange, long startedAt) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/api/") || path.startsWith("/internal/")) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            logger.info(
                    "HTTP request completed method={} path={} status={} durationMs={}",
                    exchange.getRequest().getMethod(),
                    path,
                    exchange.getResponse().getStatusCode(),
                    durationMs
            );
        }
    }
}
