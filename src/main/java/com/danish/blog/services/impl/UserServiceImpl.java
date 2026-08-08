package com.danish.blog.services.impl;

import com.danish.blog.entities.Role;
import com.danish.blog.entities.User;
import com.danish.blog.exceptions.ApiException;
import com.danish.blog.exceptions.ResourceNotFoundException;
import com.danish.blog.payloads.AppConstants;
import com.danish.blog.payloads.RoleDto;
import com.danish.blog.payloads.UserRegistrationRequest;
import com.danish.blog.payloads.UserResponse;
import com.danish.blog.payloads.UserUpdateRequest;
import com.danish.blog.repositories.RoleRepo;
import com.danish.blog.repositories.UserRepo;
import com.danish.blog.security.AuthenticatedUser;
import com.danish.blog.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepo roleRepo;

    public UserServiceImpl(UserRepo userRepo, PasswordEncoder passwordEncoder, RoleRepo roleRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.roleRepo = roleRepo;
    }

    @Override
    public UserResponse registerUser(UserRegistrationRequest request) {
        User user = newUser(request);
        user.getRoles().add(normalUserRole());
        return toResponse(userRepo.save(user));
    }

    @Override
    public UserResponse createUser(UserRegistrationRequest request) {
        User user = newUser(request);
        user.getRoles().add(normalUserRole());
        return toResponse(userRepo.save(user));
    }

    @Override
    public UserResponse updateUser(UserUpdateRequest request, Integer userId, AuthenticatedUser actor) {
        requireOwnerOrAdmin(userId, actor);
        User user = findUser(userId);
        user.setName(request.getName().trim());
        user.setEmail(normalizeEmail(request.getEmail()));
        user.setAbout(request.getAbout().trim());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(userRepo.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer userId) {
        User user = findUser(userId);
        logger.debug("Fetched user by id: {}", userId);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepo.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ApiException("Authenticated user profile was not found"));
        logger.debug("Fetched authenticated user profile for email: {}", email);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public void deleteUser(Integer userId) {
        userRepo.delete(findUser(userId));
    }

    private User newUser(UserRegistrationRequest request) {
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(normalizeEmail(request.getEmail()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAbout(request.getAbout().trim());
        return user;
    }

    private Role normalUserRole() {
        return roleRepo.findById(AppConstants.NORMAL_USER)
                .orElseThrow(() -> new ApiException("Default user role is not configured"));
    }

    private User findUser(Integer userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
    }

    private void requireOwnerOrAdmin(Integer userId, AuthenticatedUser actor) {
        if (actor == null || !actor.canManage(userId)) {
            throw new AccessDeniedException("You cannot update another user's profile");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setAbout(user.getAbout());
        response.setRoles(toRoleDtos(user.getRoles()));
        return response;
    }

    private Set<RoleDto> toRoleDtos(Set<Role> roles) {
        Set<RoleDto> roleDtos = new HashSet<>();
        for (Role role : roles) {
            RoleDto roleDto = new RoleDto();
            roleDto.setId(role.getId());
            roleDto.setName(role.getName());
            roleDtos.add(roleDto);
        }
        return roleDtos;
    }
}
