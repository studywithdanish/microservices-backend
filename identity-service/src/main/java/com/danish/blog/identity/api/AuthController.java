package com.danish.blog.identity.api;

import com.danish.blog.identity.domain.User;
import com.danish.blog.identity.error.ApiException;
import com.danish.blog.identity.security.CurrentUserProvider;
import com.danish.blog.identity.security.IdentityUserDetailsService;
import com.danish.blog.identity.security.JwtPrincipal;
import com.danish.blog.identity.security.JwtTokenService;
import com.danish.blog.identity.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final IdentityUserDetailsService userDetailsService;
    private final JwtTokenService jwtTokenService;
    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(
            AuthenticationManager authenticationManager,
            IdentityUserDetailsService userDetailsService,
            JwtTokenService jwtTokenService,
            UserService userService,
            CurrentUserProvider currentUserProvider
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtTokenService = jwtTokenService;
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@Valid @RequestBody JwtAuthRequest request) {
        String email = request.username().trim().toLowerCase(Locale.ROOT);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (BadCredentialsException exception) {
            logger.warn("Failed login attempt for username: {}", email);
            throw new ApiException("Invalid username or password");
        }

        User user = (User) userDetailsService.loadUserByUsername(email);
        return ResponseEntity.ok(new JwtAuthResponse(jwtTokenService.generateToken(user)));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser(Authentication authentication) {
        JwtPrincipal principal = currentUserProvider.requireCurrentUser(authentication);
        return ResponseEntity.ok(userService.getById(principal.id()));
    }
}
