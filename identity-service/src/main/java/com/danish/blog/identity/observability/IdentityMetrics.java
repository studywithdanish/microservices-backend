package com.danish.blog.identity.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class IdentityMetrics {

    private final Counter successfulLogins;
    private final Counter failedLogins;
    private final Counter registeredUsers;

    public IdentityMetrics(MeterRegistry registry) {
        successfulLogins = Counter.builder("blog.auth.login.attempts")
                .description("Authentication attempts by outcome")
                .tag("outcome", "success")
                .register(registry);
        failedLogins = Counter.builder("blog.auth.login.attempts")
                .description("Authentication attempts by outcome")
                .tag("outcome", "failure")
                .register(registry);
        registeredUsers = Counter.builder("blog.users.registered")
                .description("Successfully registered users")
                .register(registry);
    }

    public void recordLoginSuccess() {
        successfulLogins.increment();
    }

    public void recordLoginFailure() {
        failedLogins.increment();
    }

    public void recordRegistration() {
        registeredUsers.increment();
    }
}
