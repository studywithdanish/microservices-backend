package com.danish.blog.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

public record JwtPrincipal(Integer id, String email, List<String> roles) implements Principal {

    public JwtPrincipal {
        roles = List.copyOf(roles);
    }

    @Override
    public String getName() {
        return email;
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }
}
