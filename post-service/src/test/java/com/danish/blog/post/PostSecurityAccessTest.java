package com.danish.blog.post;

import com.danish.blog.post.security.InternalServiceAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostSecurityAccessTest {

    private static final String INTERNAL_TOKEN =
            "test-internal-service-token-test-internal-service-token-test-internal-service-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicPostListingRemainsAvailable() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void anonymousPostCreationIsRejected() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Phase 4\",\"content\":\"Extract posts\",\"categoryId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalReferenceRejectsMissingServiceCredential() throws Exception {
        mockMvc.perform(get("/internal/posts/99/reference"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalReferenceAcceptsServiceCredential() throws Exception {
        mockMvc.perform(get("/internal/posts/99/reference")
                        .header(InternalServiceAuthenticationFilter.INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN))
                .andExpect(status().isNotFound());
    }
}
