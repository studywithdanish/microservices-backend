package com.danish.blog.content.api;

import com.danish.blog.content.security.AuthenticatedUserProvider;
import com.danish.blog.content.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public CommentController(CommentService commentService, AuthenticatedUserProvider authenticatedUserProvider) {
        this.commentService = commentService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping({"/posts/{postId}/comments", "/comments/post/{postId}/comments"})
    public ResponseEntity<CommentDto> create(
            @Valid @RequestBody CommentCreateRequest request,
            @PathVariable Integer postId,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(
                request,
                postId,
                authenticatedUserProvider.requireCurrentUser(authentication)
        ));
    }

    @GetMapping({"/posts/{postId}/comments", "/comments/post/{postId}/comments"})
    public ResponseEntity<List<CommentDto>> getByPost(@PathVariable Integer postId) {
        return ResponseEntity.ok(commentService.getByPost(postId));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Integer commentId, Authentication authentication) {
        commentService.delete(commentId, authenticatedUserProvider.requireCurrentUser(authentication));
        return ResponseEntity.ok(new ApiResponse("Comment deleted successfully", true));
    }
}
