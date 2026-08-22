package com.danish.blog.identity.service;

import com.danish.blog.identity.api.RoleResponse;
import com.danish.blog.identity.api.UserRegistrationRequest;
import com.danish.blog.identity.api.UserResponse;
import com.danish.blog.identity.api.UserUpdateRequest;
import com.danish.blog.identity.domain.Role;
import com.danish.blog.identity.domain.User;
import com.danish.blog.identity.error.ApiException;
import com.danish.blog.identity.error.EmailAlreadyExistsException;
import com.danish.blog.identity.error.ResourceNotFoundException;
import com.danish.blog.identity.repository.RoleRepository;
import com.danish.blog.identity.repository.UserRepository;
import com.danish.blog.identity.security.JwtPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    static final int NORMAL_USER_ROLE_ID = 502;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(UserRegistrationRequest request) {
        return createNormalUser(request);
    }

    public UserResponse create(UserRegistrationRequest request) {
        return createNormalUser(request);
    }

    public UserResponse update(UserUpdateRequest request, Integer userId, JwtPrincipal actor) {
        if (actor == null || !actor.canManage(userId)) {
            throw new AccessDeniedException("You cannot update another user's profile");
        }

        User user = findUser(userId);
        String email = normalizeEmail(request.email());
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setAbout(request.about().trim());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Integer userId) {
        return toResponse(findUser(userId));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public void delete(Integer userId) {
        userRepository.delete(findUser(userId));
    }

    private UserResponse createNormalUser(UserRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        Role normalRole = roleRepository.findById(NORMAL_USER_ROLE_ID)
                .orElseThrow(() -> new ApiException("Default user role is not configured"));
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setAbout(request.about().trim());
        user.getRoles().add(normalRole);
        return toResponse(userRepository.save(user));
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        Set<RoleResponse> roles = user.getRoles().stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toUnmodifiableSet());
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getAbout(), roles);
    }

    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(role.getId(), role.getName());
    }
}
