package com.danish.blog.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JwtAuthRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email address is not valid")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}
