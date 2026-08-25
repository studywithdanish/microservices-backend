package com.danish.blog.content.client;

import com.danish.blog.content.error.DownstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpPostReferenceClient implements PostReferenceClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestClient restClient;
    private final String internalServiceToken;

    public HttpPostReferenceClient(
            RestClient.Builder builder,
            @Value("${app.post-service.base-url}") String baseUrl,
            @Value("${app.internal.service-token}") String internalServiceToken
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalServiceToken = internalServiceToken;
    }

    @Override
    public boolean existsById(Integer postId) {
        try {
            PostReference reference = restClient.get()
                    .uri("/internal/posts/{postId}/reference", postId)
                    .header(INTERNAL_TOKEN_HEADER, internalServiceToken)
                    .retrieve()
                    .body(PostReference.class);
            return reference != null && postId.equals(reference.postId());
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw new DownstreamServiceException("Post Service is unavailable", exception);
        } catch (RestClientException exception) {
            throw new DownstreamServiceException("Post Service is unavailable", exception);
        }
    }
}
