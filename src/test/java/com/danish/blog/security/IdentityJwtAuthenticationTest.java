package com.danish.blog.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityJwtAuthenticationTest {

    private static final String SECRET =
            "test-secret-for-automated-tests-only-test-secret-for-automated-tests-only";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesIdentityTokenWithoutLoadingAUserDatabaseRecord() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtTokenHelper(SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/posts");
        request.addHeader("Authorization", "Bearer " + token(42, "danish@example.com", "ROLE_NORMAL"));

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(
                    new JwtPrincipal(42, "danish@example.com", List.of("ROLE_NORMAL"))
            );
        });
    }

    @Test
    void exposesIdentityClaimsToOwnershipAuthorization() {
        JwtPrincipal principal = new JwtTokenHelper(SECRET)
                .parseToken(token(7, "admin@example.com", "ROLE_ADMIN"));
        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.authorities()
        );

        AuthenticatedUser actor = new AuthenticatedUserProvider().getCurrentUser(authentication);

        assertThat(actor.id()).isEqualTo(7);
        assertThat(actor.email()).isEqualTo("admin@example.com");
        assertThat(actor.admin()).isTrue();
    }

    private String token(int userId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(JwtTokenHelper.ISSUER)
                .subject(email)
                .claims(Map.of(
                        JwtTokenHelper.USER_ID_CLAIM, userId,
                        JwtTokenHelper.ROLES_CLAIM, List.of(role)
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512)
                .compact();
    }
}
