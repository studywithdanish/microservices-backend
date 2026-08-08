package com.danish.blog.controllers;

import com.danish.blog.payloads.ApiResponse;
import com.danish.blog.payloads.CommentCreateRequest;
import com.danish.blog.payloads.CommentDto;
import com.danish.blog.security.AuthenticatedUser;
import com.danish.blog.security.AuthenticatedUserProvider;
import com.danish.blog.services.CommentService;
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

    public CommentController(
            CommentService commentService,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.commentService = commentService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping({"/posts/{postId}/comments", "/comments/post/{postId}/comments"})
    public ResponseEntity<CommentDto> createComment(
            @Valid @RequestBody CommentCreateRequest request,
            @PathVariable Integer postId,
            Authentication authentication
    ) {
        AuthenticatedUser actor = authenticatedUserProvider.getCurrentUser(authentication);
        return new ResponseEntity<>(commentService.createComment(request, postId, actor), HttpStatus.CREATED);
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentDto>> getCommentsByPost(@PathVariable Integer postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse> deleteComment(
            @PathVariable Integer commentId,
            Authentication authentication
    ) {
        commentService.deleteComment(commentId, authenticatedUserProvider.getCurrentUser(authentication));
        return ResponseEntity.ok(new ApiResponse("Comment deleted successfully", true));
    }
}
