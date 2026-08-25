package com.danish.blog.content.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenVerifierTest {

    private static final String SECRET =
            "test-secret-for-automated-tests-only-test-secret-for-automated-tests-only";

    private final JwtTokenVerifier verifier = new JwtTokenVerifier(SECRET);

    @Test
    void parsesIdentityClaims() {
        String token = Jwts.builder()
                .issuer(JwtTokenVerifier.ISSUER)
                .subject("danish@example.com")
                .claim(JwtTokenVerifier.USER_ID_CLAIM, 7)
                .claim(JwtTokenVerifier.ROLES_CLAIM, List.of("ROLE_NORMAL"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        JwtPrincipal principal = verifier.parseToken(token);

        assertThat(principal.id()).isEqualTo(7);
        assertThat(principal.email()).isEqualTo("danish@example.com");
        assertThat(principal.roles()).containsExactly("ROLE_NORMAL");
    }

    @Test
    void rejectsWrongIssuer() {
        String token = Jwts.builder()
                .issuer("untrusted-service")
                .subject("danish@example.com")
                .claim(JwtTokenVerifier.USER_ID_CLAIM, 7)
                .claim(JwtTokenVerifier.ROLES_CLAIM, List.of("ROLE_NORMAL"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> verifier.parseToken(token)).isInstanceOf(RuntimeException.class);
    }
}
