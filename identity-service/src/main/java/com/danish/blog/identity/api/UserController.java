package com.danish.blog.identity.api;

import com.danish.blog.identity.security.CurrentUserProvider;
import com.danish.blog.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> update(
            @Valid @RequestBody UserUpdateRequest request,
            @PathVariable Integer userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userService.update(
                request,
                userId,
                currentUserProvider.requireCurrentUser(authentication)
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Integer userId) {
        userService.delete(userId);
        return ResponseEntity.ok(new ApiResponse("User Deleted Successfully!!", true));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getById(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getById(userId));
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }
}
