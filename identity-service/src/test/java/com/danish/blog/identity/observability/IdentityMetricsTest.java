package com.danish.blog.identity.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityMetricsTest {

    @Test
    void recordsAuthenticationAndRegistrationOutcomes() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        IdentityMetrics metrics = new IdentityMetrics(registry);

        metrics.recordLoginSuccess();
        metrics.recordLoginFailure();
        metrics.recordRegistration();

        assertThat(registry.counter("blog.auth.login.attempts", "outcome", "success").count()).isEqualTo(1);
        assertThat(registry.counter("blog.auth.login.attempts", "outcome", "failure").count()).isEqualTo(1);
        assertThat(registry.counter("blog.users.registered").count()).isEqualTo(1);
    }
}
