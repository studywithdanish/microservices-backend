package com.danish.blog.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 100, message = "Username must be between 4 and 100 characters")
    private String name;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email address is not valid")
    private String email;

    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @NotBlank(message = "About is required")
    @Size(max = 500, message = "About must not exceed 500 characters")
    private String about;
}
