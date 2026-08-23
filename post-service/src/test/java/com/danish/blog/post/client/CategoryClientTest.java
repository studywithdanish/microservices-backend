package com.danish.blog.post.client;

import com.danish.blog.post.api.CategorySnapshot;
import com.danish.blog.post.error.DownstreamServiceException;
import com.danish.blog.post.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CategoryClientTest {

    private MockRestServiceServer server;
    private CategoryClient categoryClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        categoryClient = new CategoryClient(builder, "http://backend:9090");
    }

    @Test
    void loadsCategorySnapshotFromOwningBackend() {
        server.expect(requestTo("http://backend:9090/api/categories/3"))
                .andRespond(withSuccess(
                        "{\"categoryId\":3,\"categoryTitle\":\"Spring\",\"categoryDescription\":\"Articles\"}",
                        MediaType.APPLICATION_JSON
                ));

        CategorySnapshot result = categoryClient.getCategory(3);

        assertThat(result.categoryTitle()).isEqualTo("Spring");
    }

    @Test
    void mapsMissingCategoryToNotFound() {
        server.expect(requestTo("http://backend:9090/api/categories/99"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> categoryClient.getCategory(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void mapsBackendFailureToServiceUnavailable() {
        server.expect(requestTo("http://backend:9090/api/categories/3"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> categoryClient.getCategory(3))
                .isInstanceOf(DownstreamServiceException.class);
    }
}
