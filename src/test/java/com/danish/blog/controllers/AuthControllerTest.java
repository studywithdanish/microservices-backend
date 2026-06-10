package com.danish.blog.controllers;

import com.danish.blog.exceptions.GlobalExceptionHandler;
import com.danish.blog.payloads.JwtAuthRequest;
import com.danish.blog.payloads.UserDto;
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
                .andExpect(jsonPath("$.message").value("Invalide Username or password !!"));
    }

    @Test
    void registerShouldReturnCreatedUser() throws Exception {
        UserDto request = userDto(0, "Danish", "danish@example.com", "pass123", "Platform learner");
        UserDto response = userDto(1, "Danish", "danish@example.com", "encoded", "Platform learner");
        when(userService.registerUser(any(UserDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("danish@example.com"));
    }

    @Test
    void registerShouldReturnBadRequestForInvalidPayload() throws Exception {
        UserDto request = userDto(0, "Dan", "invalid-email", "pw", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Username must be at least 4 characters"))
                .andExpect(jsonPath("$.email").value("Email address is not valid"))
                .andExpect(jsonPath("$.about").exists());
    }

    private UserDto userDto(int id, String name, String email, String password, String about) {
        UserDto userDto = new UserDto();
        userDto.setId(id);
        userDto.setName(name);
        userDto.setEmail(email);
        userDto.setPassword(password);
        userDto.setAbout(about);
        return userDto;
    }
}
