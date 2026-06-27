package com.danish.blog.security;

import com.danish.blog.payloads.CategoryDto;
import com.danish.blog.payloads.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void actuatorHealthShouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UP")));
    }

    @Test
    void swaggerApiDocsShouldBePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void categoryGetEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/categories/"))
                .andExpect(status().isOk());
    }

    @Test
    void authRegisterEndpointShouldBePublic() throws Exception {
        UserDto request = new UserDto();
        request.setName("Dan");
        request.setEmail("invalid-email");
        request.setPassword("pw");
        request.setAbout("");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email address is not valid"));
    }

    @Test
    void currentUserEndpointShouldRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedUserEndpointShouldRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/users/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "NORMAL")
    void protectedUserEndpointShouldAllowAuthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/users/"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "NORMAL")
    void adminDeleteEndpointShouldRejectNonAdminUsers() throws Exception {
        mockMvc.perform(delete("/api/users/{userId}", 1))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedCategoryCreateEndpointShouldRejectAnonymousRequests() throws Exception {
        CategoryDto request = new CategoryDto();
        request.setCategoryTitle("Cloud");
        request.setCategoryDescription("Cloud platform articles");

        mockMvc.perform(post("/api/categories/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
