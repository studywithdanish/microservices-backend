package com.danish.blog.identity;

import com.danish.blog.identity.domain.Role;
import com.danish.blog.identity.domain.User;
import com.danish.blog.identity.repository.RoleRepository;
import com.danish.blog.identity.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetIdentityData() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(role(501, "ROLE_ADMIN"));
        roleRepository.save(role(502, "ROLE_NORMAL"));
    }

    @Test
    void registerLoginAndCurrentProfileKeepTheExistingPublicContract() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration("danish@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("danish@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        String token = login("danish@example.com", "password123");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("danish@example.com"))
                .andExpect(jsonPath("$.roles[0].name").value("ROLE_NORMAL"));
    }

    @Test
    void duplicateRegistrationReturnsConflict() throws Exception {
        userRepository.save(user("danish@example.com", "ROLE_NORMAL"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration("DANISH@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void userAdministrationRequiresAdministratorRole() throws Exception {
        userRepository.save(user("normal@example.com", "ROLE_NORMAL"));
        userRepository.save(user("admin@example.com", "ROLE_ADMIN"));

        String normalToken = login("normal@example.com", "password123");
        mockMvc.perform(get("/api/users/").header("Authorization", "Bearer " + normalToken))
                .andExpect(status().isForbidden());

        String adminToken = login("admin@example.com", "password123");
        mockMvc.perform(get("/api/users/").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void currentProfileRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }

    private Map<String, String> registration(String email) {
        return Map.of(
                "name", "Danish Khan",
                "email", email,
                "password", "password123",
                "about", "Platform engineer"
        );
    }

    private User user(String email, String roleName) {
        Role role = roleRepository.getReferenceById("ROLE_ADMIN".equals(roleName) ? 501 : 502);
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setAbout("Test profile");
        user.getRoles().add(role);
        return user;
    }

    private Role role(int id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }
}
