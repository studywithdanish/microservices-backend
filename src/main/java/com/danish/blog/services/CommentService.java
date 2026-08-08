package com.danish.blog.services;

import com.danish.blog.payloads.CommentCreateRequest;
import com.danish.blog.payloads.CommentDto;
import com.danish.blog.security.AuthenticatedUser;

import java.util.List;

public interface CommentService {

    CommentDto createComment(CommentCreateRequest request, Integer postId, AuthenticatedUser actor);

    List<CommentDto> getCommentsByPost(Integer postId);

    void deleteComment(Integer commentId, AuthenticatedUser actor);
}
