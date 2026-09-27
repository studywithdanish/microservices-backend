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

    private static HttpServer identityService;
    private static HttpServer postService;
    private static HttpServer contentService;
    private static HttpServer notificationService;

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
        if (identityService != null) {
            identityService.stop(0);
        }
        if (postService != null) {
            postService.stop(0);
        }
        if (contentService != null) {
            contentService.stop(0);
        }
        if (notificationService != null) {
            notificationService.stop(0);
        }
    }

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        ensureBackendStarted();
        registry.add(
                "IDENTITY_BASE_URL",
                () -> "http://localhost:" + identityService.getAddress().getPort()
        );
        registry.add(
                "POST_SERVICE_BASE_URL",
                () -> "http://localhost:" + postService.getAddress().getPort()
        );
        registry.add(
                "CONTENT_SERVICE_BASE_URL",
                () -> "http://localhost:" + contentService.getAddress().getPort()
        );
        registry.add(
                "NOTIFICATION_SERVICE_BASE_URL",
                () -> "http://localhost:" + notificationService.getAddress().getPort()
        );
        registry.add(
                "CORS_ALLOWED_ORIGINS",
                () -> "http://localhost:3000,http://localhost:5173"
        );
    }

    @Test
    void routesPostRequestAndPreservesSecurityHeaders() {
        webTestClient.get()
                .uri("/api/posts?pageNo=0")
                .header(HttpHeaders.AUTHORIZATION, "Bearer phase-2-token")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "request-123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIdFilter.CORRELATION_ID_HEADER, "request-123")
                .expectBody(String.class)
                .isEqualTo("post|GET|/api/posts?pageNo=0|Bearer phase-2-token|request-123");
    }

    @Test
    void routesCommentAndCategoryApisToContentService() {
        webTestClient.get()
                .uri("/api/posts/10/comments")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body)
                        .startsWith("content|GET|/api/posts/10/comments|"));

        webTestClient.get()
                .uri("/api/categories/10")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body)
                        .startsWith("content|GET|/api/categories/10|"));
    }

    @Test
    void preservesIdentityTokenWhenRoutingWritesToContentService() {
        webTestClient.post()
                .uri("/api/posts/10/comments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer identity-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body)
                        .startsWith("content|POST|/api/posts/10/comments|Bearer identity-token|"));
    }

    @Test
    void routesAuthenticationAndUserApisToIdentityService() {
        webTestClient.post()
                .uri("/api/v1/auth/register")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "identity-request")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("identity|POST|/api/v1/auth/register||identity-request");

        webTestClient.get()
                .uri("/api/users/7")
                .header(HttpHeaders.AUTHORIZATION, "Bearer identity-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body)
                        .startsWith("identity|GET|/api/users/7|Bearer identity-token|"));
    }

    @Test
    void routesAuthenticatedNotificationApisToNotificationService() {
        webTestClient.get()
                .uri("/api/notifications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer identity-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body)
                        .startsWith("notification|GET|/api/notifications|Bearer identity-token|"));
    }

    @Test
    void convertsTheHttpOnlyAuthenticationCookieToABearerHeader() {
        webTestClient.get()
                .uri("/api/notifications")
                .cookie("BLOG_ACCESS_TOKEN", "cookie-token")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist("X-Received-Cookie")
                .expectBody(String.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body)
                        .startsWith("notification|GET|/api/notifications|Bearer cookie-token|"));
    }

    @Test
    void preservesAnExplicitBearerHeaderWhenACookieIsAlsoPresent() {
        webTestClient.get()
                .uri("/api/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer explicit-token")
                .cookie("BLOG_ACCESS_TOKEN", "cookie-token")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist("X-Received-Cookie")
                .expectBody(String.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body)
                        .startsWith("post|GET|/api/posts|Bearer explicit-token|"));
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
        if (identityService != null) {
            return;
        }
        try {
            identityService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            identityService.createContext("/", exchange -> echoRequest(exchange, "identity"));
            identityService.start();
            postService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            postService.createContext("/", exchange -> echoRequest(exchange, "post"));
            postService.start();
            contentService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            contentService.createContext("/", exchange -> echoRequest(exchange, "content"));
            contentService.start();
            notificationService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            notificationService.createContext("/", exchange -> echoRequest(exchange, "notification"));
            notificationService.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start test backend", exception);
        }
    }

    private static void echoRequest(HttpExchange exchange, String service) throws IOException {
        String authorization = exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String correlationId = exchange.getRequestHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        String body = String.join(
                "|",
                service,
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                valueOrEmpty(authorization),
                valueOrEmpty(correlationId)
        );
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
        String cookie = exchange.getRequestHeaders().getFirst(HttpHeaders.COOKIE);
        if (cookie != null) {
            exchange.getResponseHeaders().set("X-Received-Cookie", cookie);
        }
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
