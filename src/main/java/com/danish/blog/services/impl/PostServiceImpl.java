package com.danish.blog.services.impl;

import com.danish.blog.entities.Category;
import com.danish.blog.entities.Post;
import com.danish.blog.exceptions.ApiException;
import com.danish.blog.exceptions.ResourceNotFoundException;
import com.danish.blog.payloads.CategoryDto;
import com.danish.blog.payloads.PostCreateRequest;
import com.danish.blog.payloads.PostDto;
import com.danish.blog.payloads.PostResponse;
import com.danish.blog.payloads.PostUpdateRequest;
import com.danish.blog.repositories.CategoryRepo;
import com.danish.blog.repositories.PostRepo;
import com.danish.blog.security.AuthenticatedUser;
import com.danish.blog.services.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepo postRepo;
    private final CategoryRepo categoryRepo;

    public PostServiceImpl(PostRepo postRepo, CategoryRepo categoryRepo) {
        this.postRepo = postRepo;
        this.categoryRepo = categoryRepo;
    }

    @Override
    public PostDto createPost(PostCreateRequest request, Integer authorId) {
        if (request.getCategoryId() == null) {
            throw new ApiException("Category id is required");
        }

        Category category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", request.getCategoryId()));
        Post post = new Post();
        post.setTitle(request.getTitle().trim());
        post.setContent(request.getContent().trim());
        post.setAuthorId(authorId);
        post.setCategory(category);
        post.setImageName("default.png");
        post.setAddedDate(new Date());
        return toDto(postRepo.save(post));
    }

    @Override
    public PostDto updatePost(PostUpdateRequest request, Integer postId, AuthenticatedUser actor) {
        Post post = findPost(postId);
        requireCanModify(post, actor);
        post.setTitle(request.getTitle().trim());
        post.setContent(request.getContent().trim());
        return toDto(postRepo.save(post));
    }

    @Override
    public PostDto updatePostImage(Integer postId, String imageName, AuthenticatedUser actor) {
        Post post = findPost(postId);
        requireCanModify(post, actor);
        post.setImageName(imageName);
        return toDto(postRepo.save(post));
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyCanModify(Integer postId, AuthenticatedUser actor) {
        requireCanModify(findPost(postId), actor);
    }

    @Override
    public void deletePost(Integer postId, AuthenticatedUser actor) {
        Post post = findPost(postId);
        requireCanModify(post, actor);
        postRepo.delete(post);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getAllPosts(Integer pageNumber, Integer pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Post> page = postRepo.findAll(pageable);

        PostResponse response = new PostResponse();
        response.setContent(page.getContent().stream().map(this::toDto).toList());
        response.setPageNo(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElement(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLastPage(page.isLast());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto getPostById(Integer postId) {
        return toDto(findPost(postId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> getPostByCategory(Integer categoryId) {
        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Category Id", categoryId));
        return postRepo.findByCategory(category).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> getPostByUser(Integer userId) {
        return postRepo.findByAuthorId(userId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> searchPosts(String keyword) {
        return postRepo.SearchByTitle("%" + keyword + "%").stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Integer postId) {
        return postRepo.existsById(postId);
    }

    private Post findPost(Integer postId) {
        return postRepo.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "PostId", postId));
    }

    private void requireCanModify(Post post, AuthenticatedUser actor) {
        if (actor == null || !actor.canManage(post.getAuthorId())) {
            throw new AccessDeniedException("You cannot modify another user's post");
        }
    }

    private PostDto toDto(Post post) {
        PostDto dto = new PostDto();
        dto.setPostId(post.getPostId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setImageName(post.getImageName());
        dto.setAddedDate(post.getAddedDate());
        dto.setAuthorId(post.getAuthorId());
        dto.setCategory(toCategoryDto(post.getCategory()));
        return dto;
    }

    private CategoryDto toCategoryDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setCategoryId(category.getCategoryId());
        dto.setCategoryTitle(category.getCategoryTitle());
        dto.setCategoryDescription(category.getCategoryDescription());
        return dto;
    }
}
