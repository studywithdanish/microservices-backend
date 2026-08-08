package com.danish.blog.security;

import java.util.Objects;

public record AuthenticatedUser(Integer id, String email, boolean admin) {

    public boolean canManage(Integer ownerId) {
        return admin || Objects.equals(id, ownerId);
    }
}
