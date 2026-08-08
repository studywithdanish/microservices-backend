package com.danish.blog.services;

import com.danish.blog.payloads.PostCreateRequest;
import com.danish.blog.payloads.PostDto;
import com.danish.blog.payloads.PostResponse;
import com.danish.blog.payloads.PostUpdateRequest;
import com.danish.blog.security.AuthenticatedUser;

import java.util.List;

public interface PostService {

    PostDto createPost(PostCreateRequest request, Integer authorId);

    PostDto updatePost(PostUpdateRequest request, Integer postId, AuthenticatedUser actor);

    PostDto updatePostImage(Integer postId, String imageName, AuthenticatedUser actor);

    void verifyCanModify(Integer postId, AuthenticatedUser actor);

    void deletePost(Integer postId, AuthenticatedUser actor);

    PostResponse getAllPosts(Integer pageNumber, Integer pageSize, String sortBy, String sortDir);

    PostDto getPostById(Integer post);

    List<PostDto> getPostByCategory(Integer categoryId);

    List<PostDto> getPostByUser(Integer userId);

    List<PostDto> searchPosts(String posts);

    boolean existsById(Integer postId);

}
