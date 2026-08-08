package com.danish.blog.services.impl;

import com.danish.blog.entities.Role;
import com.danish.blog.entities.User;
import com.danish.blog.exceptions.ResourceNotFoundException;
import com.danish.blog.payloads.AppConstants;
import com.danish.blog.payloads.UserRegistrationRequest;
import com.danish.blog.payloads.UserResponse;
import com.danish.blog.payloads.UserUpdateRequest;
import com.danish.blog.repositories.RoleRepo;
import com.danish.blog.repositories.UserRepo;
import com.danish.blog.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepo roleRepo;

    private UserServiceImpl userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepo, passwordEncoder, roleRepo);
        user = new User();
        user.setId(1);
        user.setName("Danish");
        user.setEmail("danish@example.com");
        user.setPassword("encoded-password");
        user.setAbout("Platform engineering learner");
    }

    @Test
    void registerUserShouldEncodePasswordAssignRoleAndReturnSafeResponse() {
        Role normalRole = normalRole();
        UserRegistrationRequest request = registrationRequest();
        when(passwordEncoder.encode("plain123")).thenReturn("encoded-password");
        when(roleRepo.findById(AppConstants.NORMAL_USER)).thenReturn(Optional.of(normalRole));
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        UserResponse response = userService.registerUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getRoles()).containsExactly(normalRole);
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getEmail()).isEqualTo("danish@example.com");
        assertThat(UserResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("password");
    }

    @Test
    void createUserShouldCreateUsableNormalAccount() {
        when(passwordEncoder.encode("plain123")).thenReturn("encoded-password");
        when(roleRepo.findById(AppConstants.NORMAL_USER)).thenReturn(Optional.of(normalRole()));
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(registrationRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getRoles())
                .extracting(Role::getName)
                .containsExactly("ROLE_NORMAL");
    }

    @Test
    void updateUserShouldAllowOwnerAndEncodeProvidedPassword() {
        UserUpdateRequest request = updateRequest("newpass123");
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded-new-password");
        when(userRepo.save(user)).thenReturn(user);

        UserResponse response = userService.updateUser(
                request,
                1,
                new AuthenticatedUser(1, "danish@example.com", false)
        );

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(response.getName()).isEqualTo("Danish Khan");
    }

    @Test
    void updateUserShouldPreservePasswordWhenItIsNotProvided() {
        UserUpdateRequest request = updateRequest(null);
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(userRepo.save(user)).thenReturn(user);

        userService.updateUser(request, 1, new AuthenticatedUser(1, "danish@example.com", false));

        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUserShouldRejectDifferentNonAdminUser() {
        assertThatThrownBy(() -> userService.updateUser(
                updateRequest(null),
                1,
                new AuthenticatedUser(2, "other@example.com", false)
        )).isInstanceOf(AccessDeniedException.class);

        verify(userRepo, never()).findById(any());
    }

    @Test
    void getUserByIdShouldReturnMappedUser() {
        when(userRepo.findById(1)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserById(1);

        assertThat(result.getEmail()).isEqualTo("danish@example.com");
        assertThat(result.getName()).isEqualTo("Danish");
    }

    @Test
    void getUserByIdShouldThrowWhenUserDoesNotExist() {
        when(userRepo.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getAllUsersShouldReturnSafeResponses() {
        when(userRepo.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("danish@example.com");
    }

    @Test
    void deleteUserShouldDeleteExistingUser() {
        when(userRepo.findById(1)).thenReturn(Optional.of(user));

        userService.deleteUser(1);

        verify(userRepo).delete(user);
    }

    private UserRegistrationRequest registrationRequest() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("Danish");
        request.setEmail("DANISH@example.com");
        request.setPassword("plain123");
        request.setAbout("Platform engineering learner");
        return request;
    }

    private UserUpdateRequest updateRequest(String password) {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Danish Khan");
        request.setEmail("danish.khan@example.com");
        request.setPassword(password);
        request.setAbout("Updated profile");
        return request;
    }

    private Role normalRole() {
        Role role = new Role();
        role.setId(AppConstants.NORMAL_USER);
        role.setName("ROLE_NORMAL");
        return role;
    }
}
