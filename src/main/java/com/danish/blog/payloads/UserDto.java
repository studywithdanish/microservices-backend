package com.danish.blog.payloads;

import javax.validation.constraints.*;

import com.danish.blog.entities.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
public class UserDto {

    private int id;

    @NotBlank(message = "Username is required")
    @Size(min = 4, message = "Username must be at least 4 characters")
    private String name;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email address is not valid")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 3, max = 10, message = "Password must be min of 3 Characters and max of 10 Characters")
    private String password;

    @NotBlank
    private String about;

    private Set<RoleDto> roles=new HashSet<>();
}
