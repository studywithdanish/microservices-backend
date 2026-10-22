package com.danish.blog.content.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void exposesSafeCorrelationIdToLogsAndResponseWithoutLeakingMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/categories/7");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "request-content-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> observedMdc = new AtomicReference<>();
        FilterChain chain = (ignoredRequest, ignoredResponse) ->
                observedMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        new CorrelationIdFilter().doFilter(request, response, chain);

        assertThat(observedMdc).hasValue("request-content-123");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("request-content-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeIncomingValue() {
        assertThat(CorrelationIdFilter.resolve("unsafe\r\nvalue"))
                .matches("[0-9a-f-]{36}");
    }
}
