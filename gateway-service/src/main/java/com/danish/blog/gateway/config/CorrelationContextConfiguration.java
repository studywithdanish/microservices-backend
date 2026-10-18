package com.danish.blog.gateway.config;

import com.danish.blog.gateway.filter.CorrelationIdFilter;
import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CorrelationContextConfiguration {

    @PostConstruct
    public void registerCorrelationMdcAccessor() {
        ContextRegistry registry = ContextRegistry.getInstance();
        registry.removeThreadLocalAccessor(CorrelationIdFilter.MDC_KEY);
        registry.registerThreadLocalAccessor(
                CorrelationIdFilter.MDC_KEY,
                () -> MDC.get(CorrelationIdFilter.MDC_KEY),
                value -> MDC.put(CorrelationIdFilter.MDC_KEY, value),
                () -> MDC.remove(CorrelationIdFilter.MDC_KEY)
        );
    }
}
