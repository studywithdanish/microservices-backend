package com.danish.blog.services;

import com.danish.blog.payloads.UserRegistrationRequest;
import com.danish.blog.payloads.UserResponse;
import com.danish.blog.payloads.UserUpdateRequest;
import com.danish.blog.security.AuthenticatedUser;

import java.util.List;

public interface UserService {

    UserResponse registerUser(UserRegistrationRequest user);
    UserResponse createUser(UserRegistrationRequest user);
    UserResponse updateUser(UserUpdateRequest user, Integer userId, AuthenticatedUser actor);
    UserResponse getUserById(Integer userId);
    UserResponse getUserByEmail(String email);
    List<UserResponse> getAllUsers();
    void deleteUser(Integer userId);
}
