package com.danish.blog.controllers;

import com.danish.blog.exceptions.GlobalExceptionHandler;
import com.danish.blog.payloads.JwtAuthRequest;
import com.danish.blog.payloads.UserRegistrationRequest;
import com.danish.blog.payloads.UserResponse;
import com.danish.blog.repositories.RoleRepo;
import com.danish.blog.security.JwtTokenHelper;
import com.danish.blog.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtTokenHelper jwtTokenHelper;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RoleRepo roleRepo;

    @Test
    void loginShouldReturnJwtToken() throws Exception {
        JwtAuthRequest request = new JwtAuthRequest();
        request.setUsername("danish@example.com");
        request.setPassword("password");

        UserDetails userDetails = new User("danish@example.com", "password", List.of());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("danish@example.com", null, List.of()));
        when(userDetailsService.loadUserByUsername("danish@example.com")).thenReturn(userDetails);
        when(jwtTokenHelper.generateToken(userDetails)).thenReturn("jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void loginShouldReturnBadRequestForInvalidCredentials() throws Exception {
        JwtAuthRequest request = new JwtAuthRequest();
        request.setUsername("danish@example.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void registerShouldReturnCreatedUser() throws Exception {
        UserRegistrationRequest request = registrationRequest(
                "Danish", "danish@example.com", "password123", "Platform learner");
        UserResponse response = userResponse(1, "Danish", "danish@example.com", "Platform learner");
        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("danish@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerShouldReturnBadRequestForInvalidPayload() throws Exception {
        UserRegistrationRequest request = registrationRequest("Dan", "invalid-email", "pw", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Username must be between 4 and 100 characters"))
                .andExpect(jsonPath("$.email").value("Email address is not valid"))
                .andExpect(jsonPath("$.about").exists());
    }

    @Test
    void currentUserShouldReturnAuthenticatedUserProfile() throws Exception {
        UserResponse response = userResponse(1, "Danish", "danish@example.com", "Platform learner");
        when(userService.getUserByEmail("danish@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/me")
                        .principal(() -> "danish@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("danish@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    private UserRegistrationRequest registrationRequest(
            String name,
            String email,
            String password,
            String about
    ) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        request.setAbout(about);
        return request;
    }

    private UserResponse userResponse(int id, String name, String email, String about) {
        UserResponse response = new UserResponse();
        response.setId(id);
        response.setName(name);
        response.setEmail(email);
        response.setAbout(about);
        return response;
    }
}
