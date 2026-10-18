package com.danish.blog.notification.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final Pattern VALID_VALUE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$");
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolve(request.getHeader(HEADER_NAME));
        long startedAt = System.nanoTime();
        response.setHeader(HEADER_NAME, correlationId);

        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                logApplicationRequest(request, response, startedAt);
            }
        }
    }

    public static String currentOrCreate() {
        return resolve(MDC.get(MDC_KEY));
    }

    public static String resolve(String candidate) {
        if (candidate != null && VALID_VALUE.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private void logApplicationRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            long startedAt
    ) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/") || path.startsWith("/internal/")) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            logger.info(
                    "HTTP request completed method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    durationMs
            );
        }
    }
}
