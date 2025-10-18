package com.danish.blog.controllers;

import com.danish.blog.payloads.ApiResponse;
import com.danish.blog.payloads.CommentDto;
import com.danish.blog.services.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/post/{postId}/comments")
    public ResponseEntity<CommentDto> createComment(@RequestBody CommentDto commentDto, @PathVariable Integer postId){
        CommentDto createdComment = commentService.createComment(commentDto, postId);
        return new ResponseEntity<CommentDto>(createdComment, HttpStatus.CREATED);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse> createComment(@PathVariable Integer commentId){
        commentService.deleteComment(commentId);
        return new ResponseEntity<ApiResponse>(new ApiResponse("Delete Successfully !!", true), HttpStatus.CREATED);
    }
}
