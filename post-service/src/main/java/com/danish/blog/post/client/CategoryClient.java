package com.danish.blog.post.client;

import com.danish.blog.post.api.CategorySnapshot;
import com.danish.blog.post.error.DownstreamServiceException;
import com.danish.blog.post.error.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CategoryClient {

    private final RestClient restClient;

    public CategoryClient(
            RestClient.Builder builder,
            @Value("${app.category-service.base-url}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public CategorySnapshot getCategory(Integer categoryId) {
        try {
            CategorySnapshot category = restClient.get()
                    .uri("/api/categories/{categoryId}", categoryId)
                    .retrieve()
                    .body(CategorySnapshot.class);
            if (category == null || category.categoryId() == null) {
                throw new DownstreamServiceException("Category service returned an invalid response", null);
            }
            return category;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Category", "Category Id", categoryId);
            }
            throw new DownstreamServiceException("Category service is unavailable", exception);
        } catch (RestClientException exception) {
            throw new DownstreamServiceException("Category service is unavailable", exception);
        }
    }
}
