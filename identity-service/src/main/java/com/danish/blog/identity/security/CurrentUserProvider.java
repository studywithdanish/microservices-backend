package com.danish.blog.identity.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public JwtPrincipal requireCurrentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AccessDeniedException("Authentication is required");
        }
        return principal;
    }
}
