package com.danish.blog.post.service;

import com.danish.blog.post.api.CategorySnapshot;
import com.danish.blog.post.api.PostCreateRequest;
import com.danish.blog.post.api.PostDto;
import com.danish.blog.post.api.PostReferenceResponse;
import com.danish.blog.post.api.PostResponse;
import com.danish.blog.post.api.PostUpdateRequest;
import com.danish.blog.post.client.CategoryClient;
import com.danish.blog.post.domain.Post;
import com.danish.blog.post.error.ApiException;
import com.danish.blog.post.error.ResourceNotFoundException;
import com.danish.blog.post.repository.PostRepository;
import com.danish.blog.post.security.JwtPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PostService {

    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "postId", "id",
            "title", "title",
            "addedDate", "addedDate",
            "authorId", "authorId",
            "categoryId", "categoryId"
    );

    private final PostRepository postRepository;
    private final CategoryClient categoryClient;
    private final PostEventOutbox postEventOutbox;

    public PostService(
            PostRepository postRepository,
            CategoryClient categoryClient,
            PostEventOutbox postEventOutbox
    ) {
        this.postRepository = postRepository;
        this.categoryClient = categoryClient;
        this.postEventOutbox = postEventOutbox;
    }

    public PostDto create(PostCreateRequest request, Integer authorId) {
        if (request.categoryId() == null) {
            throw new ApiException("Category id is required");
        }

        CategorySnapshot category = categoryClient.getCategory(request.categoryId());
        Post post = new Post();
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setAuthorId(authorId);
        post.setCategoryId(category.categoryId());
        post.setCategoryTitle(category.categoryTitle());
        post.setCategoryDescription(category.categoryDescription());
        post.setImageName("default.png");
        post.setAddedDate(new Date());
        Post saved = postRepository.save(post);
        postEventOutbox.recordPostPublished(saved);
        return toDto(saved);
    }

    public PostDto update(PostUpdateRequest request, Integer postId, JwtPrincipal actor) {
        Post post = findPost(postId);
        requireCanModify(post, actor);
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        return toDto(postRepository.save(post));
    }

    public PostDto updateImage(Integer postId, String imageName, JwtPrincipal actor) {
        Post post = findPost(postId);
        requireCanModify(post, actor);
        post.setImageName(imageName);
        return toDto(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public void verifyCanModify(Integer postId, JwtPrincipal actor) {
        requireCanModify(findPost(postId), actor);
    }

    public void delete(Integer postId, JwtPrincipal actor) {
        Post post = findPost(postId);
        requireCanModify(post, actor);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public PostResponse getAll(Integer pageNumber, Integer pageSize, String sortBy, String sortDirection) {
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw new ApiException("Page number must be non-negative and page size must be between 1 and 100");
        }
        String property = SORT_PROPERTIES.get(sortBy);
        if (property == null) {
            throw new ApiException("Unsupported post sort property");
        }
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(property).ascending()
                : Sort.by(property).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Post> page = postRepository.findAll(pageable);
        return new PostResponse(
                page.getContent().stream().map(this::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PostDto getById(Integer postId) {
        return toDto(findPost(postId));
    }

    @Transactional(readOnly = true)
    public List<PostDto> getByCategory(Integer categoryId) {
        categoryClient.getCategory(categoryId);
        return postRepository.findByCategoryId(categoryId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PostDto> getByAuthor(Integer authorId) {
        return postRepository.findByAuthorId(authorId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PostDto> search(String keyword) {
        return postRepository.findByTitleContainingIgnoreCase(keyword).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PostReferenceResponse getReference(Integer postId) {
        Post post = findPost(postId);
        return new PostReferenceResponse(post.getId(), post.getAuthorId());
    }

    private Post findPost(Integer postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "PostId", postId));
    }

    private void requireCanModify(Post post, JwtPrincipal actor) {
        if (actor == null || !actor.canManage(post.getAuthorId())) {
            throw new AccessDeniedException("You cannot modify another user's post");
        }
    }

    private PostDto toDto(Post post) {
        return new PostDto(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getImageName(),
                post.getAddedDate(),
                new CategorySnapshot(
                        post.getCategoryId(),
                        post.getCategoryTitle(),
                        post.getCategoryDescription()
                ),
                post.getAuthorId()
        );
    }
}
