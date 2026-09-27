package com.danish.blog.identity.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final Duration lifetime;

    public AuthCookieService(
            @Value("${app.auth.cookie-name:BLOG_ACCESS_TOKEN}") String cookieName,
            @Value("${app.auth.cookie-secure:false}") boolean secure,
            @Value("${app.auth.cookie-same-site:Lax}") String sameSite,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.lifetime = Duration.ofMillis(expirationMs);
    }

    public ResponseCookie authenticated(String token) {
        return baseCookie(token)
                .maxAge(lifetime)
                .build();
    }

    public ResponseCookie cleared() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }
}
