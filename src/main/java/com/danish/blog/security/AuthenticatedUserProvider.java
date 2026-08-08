package com.danish.blog.security;

import com.danish.blog.entities.User;
import com.danish.blog.repositories.UserRepo;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    private final UserRepo userRepo;

    public AuthenticatedUserProvider(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public AuthenticatedUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        User user = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user profile was not found"));
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        return new AuthenticatedUser(user.getId(), user.getEmail(), admin);
    }
}
