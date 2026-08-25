package com.danish.blog.content.service;

import com.danish.blog.content.api.CommentCreateRequest;
import com.danish.blog.content.api.CommentDto;
import com.danish.blog.content.security.AuthenticatedUser;

import java.util.List;

public interface CommentService {

    CommentDto create(CommentCreateRequest request, Integer postId, AuthenticatedUser actor);

    List<CommentDto> getByPost(Integer postId);

    void delete(Integer commentId, AuthenticatedUser actor);
}
