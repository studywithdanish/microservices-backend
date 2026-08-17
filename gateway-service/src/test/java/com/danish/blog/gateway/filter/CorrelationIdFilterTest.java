package com.danish.blog.gateway.filter;

import org.junit.jupiter.api.Test;

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
}
