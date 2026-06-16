package com.danish.blog.entities;

import jakarta.persistence.ManyToMany;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMappingTest {

    @Test
    void userRolesShouldNotCascadeRolePersistence() throws NoSuchFieldException {
        ManyToMany rolesMapping = User.class
                .getDeclaredField("roles")
                .getAnnotation(ManyToMany.class);

        assertThat(rolesMapping.cascade()).isEmpty();
    }
}
