package com.danish.blog.gateway;

import com.danish.blog.gateway.filter.CorrelationIdFilter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApiGatewayRoutingTest {

    private static HttpServer backend;

    @Autowired
    private WebTestClient webTestClient;

    @LocalServerPort
    private int gatewayPort;

    @BeforeAll
    static void startBackend() {
        ensureBackendStarted();
    }

    @AfterAll
    static void stopBackend() {
        if (backend != null) {
            backend.stop(0);
        }
    }

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        ensureBackendStarted();
        registry.add(
                "BACKEND_BASE_URL",
                () -> "http://localhost:" + backend.getAddress().getPort()
        );
        registry.add(
                "CORS_ALLOWED_ORIGINS",
                () -> "http://localhost:3000,http://localhost:5173"
        );
    }

    @Test
    void routesApiRequestAndPreservesSecurityHeaders() {
        webTestClient.get()
                .uri("/api/posts?pageNo=0")
                .header(HttpHeaders.AUTHORIZATION, "Bearer phase-2-token")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "request-123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIdFilter.CORRELATION_ID_HEADER, "request-123")
                .expectBody(String.class)
                .isEqualTo("GET|/api/posts?pageNo=0|Bearer phase-2-token|request-123");
    }

    @Test
    void generatesCorrelationIdWhenClientDoesNotProvideOne() {
        webTestClient.get()
                .uri("/api/posts")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches(
                        CorrelationIdFilter.CORRELATION_ID_HEADER,
                        "[0-9a-f-]{36}"
                );
    }

    @Test
    void doesNotExposeUnconfiguredRoutes() {
        webTestClient.get()
                .uri("/internal/not-routed")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void exposesGatewayHealthEndpoint() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void handlesCorsPreflightAtTheGatewayBoundary() {
        webTestClient.options()
                .uri("http://localhost:" + gatewayPort + "/api/posts")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:3000"
                )
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
    }

    private static void ensureBackendStarted() {
        if (backend != null) {
            return;
        }
        try {
            backend = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            backend.createContext("/", ApiGatewayRoutingTest::echoRequest);
            backend.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start test backend", exception);
        }
    }

    private static void echoRequest(HttpExchange exchange) throws IOException {
        String authorization = exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String correlationId = exchange.getRequestHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        String body = String.join(
                "|",
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                valueOrEmpty(authorization),
                valueOrEmpty(correlationId)
        );
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
