package com.danish.blog.services;

import com.danish.blog.entities.Post;
import com.danish.blog.payloads.PostDto;
import com.danish.blog.payloads.PostResponse;

import java.util.List;

public interface PostService {

    PostDto createPost(PostDto postDto, Integer userId, Integer categoryId);

    PostDto updatePost(PostDto postDto, Integer postId);

    void deletePost(Integer postId);

    PostResponse getAllPosts(Integer pageNumber, Integer pageSize, String sortBy, String sortDir);

    PostDto getPostById(Integer post);

    List<PostDto> getPostByCategory(Integer categoryId);

    List<PostDto> getPostByUser(Integer userId);

    List<PostDto> searchPosts(String posts);

}
