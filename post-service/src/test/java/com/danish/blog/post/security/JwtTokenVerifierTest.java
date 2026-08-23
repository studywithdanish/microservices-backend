package com.danish.blog.post.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenVerifierTest {

    private static final String SECRET =
            "test-secret-for-automated-tests-only-test-secret-for-automated-tests-only";

    @Test
    void acceptsIdentityTokenWithOwnershipClaims() {
        JwtPrincipal principal = new JwtTokenVerifier(SECRET).parseToken(token(42, "ROLE_ADMIN", SECRET));

        assertThat(principal.id()).isEqualTo(42);
        assertThat(principal.email()).isEqualTo("danish@example.com");
        assertThat(principal.canManage(7)).isTrue();
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String otherSecret =
                "other-secret-for-automated-tests-only-other-secret-for-automated-tests-only";

        assertThatThrownBy(() -> new JwtTokenVerifier(SECRET).parseToken(
                token(42, "ROLE_NORMAL", otherSecret)
        )).isInstanceOf(JwtException.class);
    }

    private String token(int userId, String role, String secret) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(JwtTokenVerifier.ISSUER)
                .subject("danish@example.com")
                .claims(Map.of(
                        JwtTokenVerifier.USER_ID_CLAIM, userId,
                        JwtTokenVerifier.ROLES_CLAIM, List.of(role)
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512)
                .compact();
    }
}
