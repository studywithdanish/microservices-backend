package com.danish.blog.identity.repository;

import com.danish.blog.identity.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
}
