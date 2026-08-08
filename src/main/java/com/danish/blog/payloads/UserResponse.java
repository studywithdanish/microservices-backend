package com.danish.blog.payloads;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
public class UserResponse {

    private int id;
    private String name;
    private String email;
    private String about;
    private Set<RoleDto> roles = new HashSet<>();
}
