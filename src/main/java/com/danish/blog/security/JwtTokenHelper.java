package com.danish.blog.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtTokenHelper {

    static final String ISSUER = "blog-identity-service";
    static final String USER_ID_CLAIM = "userId";
    static final String ROLES_CLAIM = "roles";

    private final SecretKey signingKey;

    public JwtTokenHelper(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtPrincipal parseToken(String token) {
        var claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Number userId = (Number) claims.get(USER_ID_CLAIM);
        Object rawRoles = claims.get(ROLES_CLAIM);
        if (userId == null || claims.getSubject() == null || !(rawRoles instanceof List<?> roleValues)) {
            throw new IllegalArgumentException("JWT is missing required identity claims");
        }

        return new JwtPrincipal(
                userId.intValue(),
                claims.getSubject(),
                roleValues.stream().map(String::valueOf).toList()
        );
    }

}
