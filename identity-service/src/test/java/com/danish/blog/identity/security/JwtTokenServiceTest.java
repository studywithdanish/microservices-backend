package com.danish.blog.identity.security;

import com.danish.blog.identity.domain.Role;
import com.danish.blog.identity.domain.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET =
            "test-secret-for-automated-tests-only-test-secret-for-automated-tests-only";

    @Test
    void tokenCarriesUserIdEmailAndRolesForDownstreamAuthorization() {
        JwtTokenService service = new JwtTokenService(
                SECRET,
                60_000,
                Clock.systemUTC()
        );
        User user = user(42, "danish@example.com", "ROLE_ADMIN");

        JwtPrincipal principal = service.parseToken(service.generateToken(user));

        assertThat(principal.id()).isEqualTo(42);
        assertThat(principal.email()).isEqualTo("danish@example.com");
        assertThat(principal.roles()).containsExactly("ROLE_ADMIN");
        assertThat(principal.isAdmin()).isTrue();
    }

    @Test
    void rejectsTokenSignedByAnotherService() {
        JwtTokenService issuer = new JwtTokenService(SECRET, 60_000, Clock.systemUTC());
        JwtTokenService consumer = new JwtTokenService(
                "different-secret-for-automated-tests-different-secret-for-automated-tests",
                60_000,
                Clock.systemUTC()
        );

        assertThatThrownBy(() -> consumer.parseToken(
                issuer.generateToken(user(1, "user@example.com", "ROLE_NORMAL"))
        )).isInstanceOf(JwtException.class);
    }

    private User user(int id, String email, String roleName) {
        Role role = new Role();
        role.setId(501);
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setName("Danish");
        user.setAbout("Platform engineer");
        user.getRoles().add(role);
        return user;
    }
}
