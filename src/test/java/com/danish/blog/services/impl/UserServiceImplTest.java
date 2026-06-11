package com.danish.blog.services.impl;

import com.danish.blog.entities.Role;
import com.danish.blog.entities.User;
import com.danish.blog.exceptions.ResourceNotFoundException;
import com.danish.blog.payloads.AppConstants;
import com.danish.blog.payloads.UserDto;
import com.danish.blog.repositories.RoleRepo;
import com.danish.blog.repositories.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepo roleRepo;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setName("Danish");
        user.setEmail("danish@example.com");
        user.setPassword("plain123");
        user.setAbout("Platform engineering learner");

        userDto = new UserDto();
        userDto.setId(1);
        userDto.setName("Danish");
        userDto.setEmail("danish@example.com");
        userDto.setPassword("plain123");
        userDto.setAbout("Platform engineering learner");
    }

    @Test
    void registerUserShouldEncodePasswordAndAssignNormalRole() {
        Role normalRole = new Role();
        normalRole.setId(AppConstants.NORMAL_USER);
        normalRole.setName("ROLE_NORMAL");

        when(modelMapper.map(userDto, User.class)).thenReturn(user);
        when(passwordEncoder.encode("plain123")).thenReturn("encoded-password");
        when(roleRepo.findById(AppConstants.NORMAL_USER)).thenReturn(Optional.of(normalRole));
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(User.class), any())).thenReturn(userDto);

        userService.registerUser(userDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRoles()).containsExactly(normalRole);
    }

    @Test
    void createUserShouldEncodePasswordBeforeSaving() {
        when(modelMapper.map(userDto, User.class)).thenReturn(user);
        when(passwordEncoder.encode("plain123")).thenReturn("encoded-password");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(User.class), any())).thenReturn(userDto);

        userService.createUser(userDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void updateUserShouldEncodeUpdatedPasswordBeforeSaving() {
        UserDto updateRequest = new UserDto();
        updateRequest.setName("Danish Khan");
        updateRequest.setEmail("danish.khan@example.com");
        updateRequest.setPassword("newpass");
        updateRequest.setAbout("Updated profile");

        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-new-password");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(User.class), any())).thenReturn(updateRequest);

        userService.updateUser(updateRequest, 1);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getName()).isEqualTo("Danish Khan");
        assertThat(savedUser.getEmail()).isEqualTo("danish.khan@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-new-password");
        assertThat(savedUser.getAbout()).isEqualTo("Updated profile");
    }

    @Test
    void getUserByIdShouldReturnMappedUser() {
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

        UserDto result = userService.getUserById(1);

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
    void getAllUsersShouldReturnMappedUsers() {
        when(userRepo.findAll()).thenReturn(List.of(user));
        when(modelMapper.map(any(User.class), any())).thenReturn(userDto);

        List<UserDto> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("danish@example.com");
    }

    @Test
    void deleteUserShouldDeleteExistingUser() {
        when(userRepo.findById(1)).thenReturn(Optional.of(user));

        userService.deleteUser(1);

        verify(userRepo).delete(user);
    }
}
