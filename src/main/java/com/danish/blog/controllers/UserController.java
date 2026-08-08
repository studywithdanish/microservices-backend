package com.danish.blog.controllers;

import com.danish.blog.payloads.ApiResponse;
import com.danish.blog.payloads.UserRegistrationRequest;
import com.danish.blog.payloads.UserResponse;
import com.danish.blog.payloads.UserUpdateRequest;
import com.danish.blog.security.AuthenticatedUser;
import com.danish.blog.security.AuthenticatedUserProvider;
import com.danish.blog.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public UserController(UserService userService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.userService = userService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping("/")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRegistrationRequest request){
        UserResponse createdUser = userService.createUser(request);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @Valid @RequestBody UserUpdateRequest request,
            @PathVariable("userId") Integer userId,
            Authentication authentication
    ) {
        AuthenticatedUser actor = authenticatedUserProvider.getCurrentUser(authentication);
        return ResponseEntity.ok(userService.updateUser(request, userId, actor));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer userId){
        userService.deleteUser(userId);
        return new ResponseEntity<ApiResponse>(new ApiResponse("User Deleted Successfully!!", true), HttpStatus.OK);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer userId){
        UserResponse userById = userService.getUserById(userId);
        return ResponseEntity.ok(userById);
    }

    @GetMapping("/")
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        List<UserResponse> allUsers = userService.getAllUsers();
        return ResponseEntity.ok(allUsers);
    }
}
