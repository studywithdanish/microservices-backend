package com.danish.blog.content.security;

import java.util.Objects;

public record AuthenticatedUser(Integer id, String email, boolean admin) {

    public boolean canManage(Integer ownerId) {
        return admin || Objects.equals(id, ownerId);
    }
}
