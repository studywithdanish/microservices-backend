package com.danish.blog.identity.api;

import java.util.Set;

public record UserResponse(int id, String name, String email, String about, Set<RoleResponse> roles) {
}
