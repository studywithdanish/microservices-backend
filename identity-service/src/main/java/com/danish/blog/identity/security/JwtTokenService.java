package com.danish.blog.identity.security;

import com.danish.blog.identity.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenService {

    public static final String ISSUER = "blog-identity-service";
    public static final String USER_ID_CLAIM = "userId";
    public static final String ROLES_CLAIM = "roles";

    private final SecretKey signingKey;
    private final long expirationMs;
    private final Clock clock;

    @Autowired
    public JwtTokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this(secret, expirationMs, Clock.systemUTC());
    }

    JwtTokenService(String secret, long expirationMs, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.clock = clock;
    }

    public String generateToken(User user) {
        Date issuedAt = Date.from(clock.instant());
        Date expiresAt = new Date(issuedAt.getTime() + expirationMs);
        List<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .sorted()
                .toList();

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getEmail())
                .claims(Map.of(USER_ID_CLAIM, user.getId(), ROLES_CLAIM, roles))
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    public JwtPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
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

        List<String> roles = roleValues.stream().map(String::valueOf).toList();
        return new JwtPrincipal(userId.intValue(), claims.getSubject(), roles);
    }
}
