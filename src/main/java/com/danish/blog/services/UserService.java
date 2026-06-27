package com.danish.blog.services;

import com.danish.blog.payloads.UserDto;

import java.util.List;

public interface UserService {

    UserDto registerUser(UserDto userDto);
    UserDto createUser(UserDto user);
    UserDto updateUser(UserDto user, Integer userId);
    UserDto getUserById(Integer userId);
    UserDto getUserByEmail(String email);
    List<UserDto> getAllUsers();
    void deleteUser(Integer userId);
}
