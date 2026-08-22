package com.danish.blog.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record JwtAuthRequest(
        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Email address is not valid")
        String username,
        @NotBlank(message = "Password cannot be empty")
        String password
) {
}
