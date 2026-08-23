package com.danish.blog.clients;

import com.danish.blog.exceptions.PostServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpPostReferenceClientTest {

    private MockRestServiceServer server;
    private HttpPostReferenceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpPostReferenceClient(builder, "http://post-service:9093", "internal-token");
    }

    @Test
    void returnsTrueForExistingPostAndSendsInternalCredential() {
        server.expect(once(), requestTo("http://post-service:9093/internal/posts/10/reference"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "internal-token"))
                .andRespond(withSuccess("{\"postId\":10,\"authorId\":1}", MediaType.APPLICATION_JSON));

        assertThat(client.existsById(10)).isTrue();
        server.verify();
    }

    @Test
    void returnsFalseWhenPostDoesNotExist() {
        server.expect(requestTo("http://post-service:9093/internal/posts/99/reference"))
                .andRespond(withResourceNotFound());

        assertThat(client.existsById(99)).isFalse();
    }

    @Test
    void convertsPostServiceFailureToStableApplicationException() {
        server.expect(requestTo("http://post-service:9093/internal/posts/10/reference"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.existsById(10))
                .isInstanceOf(PostServiceUnavailableException.class)
                .hasMessage("Post Service is unavailable");
    }
}
