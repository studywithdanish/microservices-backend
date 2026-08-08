package com.danish.blog.services.impl;

import com.danish.blog.entities.Category;
import com.danish.blog.entities.Post;
import com.danish.blog.payloads.PostCreateRequest;
import com.danish.blog.payloads.PostDto;
import com.danish.blog.payloads.PostUpdateRequest;
import com.danish.blog.repositories.CategoryRepo;
import com.danish.blog.repositories.PostRepo;
import com.danish.blog.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepo postRepo;

    @Mock
    private CategoryRepo categoryRepo;

    private PostServiceImpl postService;
    private Category category;
    private Post post;

    @BeforeEach
    void setUp() {
        postService = new PostServiceImpl(postRepo, categoryRepo);
        category = new Category();
        category.setCategoryId(3);
        category.setCategoryTitle("Spring");
        category.setCategoryDescription("Spring Boot articles");

        post = new Post();
        post.setPostId(10);
        post.setTitle("Secure services");
        post.setContent("Service boundary content");
        post.setAuthorId(1);
        post.setCategory(category);
        post.setImageName("default.png");
    }

    @Test
    void createPostShouldUseAuthenticatedAuthorId() {
        PostCreateRequest request = createRequest();
        when(categoryRepo.findById(3)).thenReturn(Optional.of(category));
        when(postRepo.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            saved.setPostId(10);
            return saved;
        });

        PostDto response = postService.createPost(request, 1);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepo).save(captor.capture());
        assertThat(captor.getValue().getAuthorId()).isEqualTo(1);
        assertThat(response.getAuthorId()).isEqualTo(1);
        assertThat(response.getCategory().getCategoryId()).isEqualTo(3);
    }

    @Test
    void updatePostShouldRejectDifferentNonAdminUser() {
        when(postRepo.findById(10)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(
                updateRequest(),
                10,
                new AuthenticatedUser(2, "other@example.com", false)
        )).isInstanceOf(AccessDeniedException.class);

        verify(postRepo, never()).save(any());
    }

    @Test
    void updatePostShouldAllowOwner() {
        when(postRepo.findById(10)).thenReturn(Optional.of(post));
        when(postRepo.save(post)).thenReturn(post);

        PostDto response = postService.updatePost(
                updateRequest(),
                10,
                new AuthenticatedUser(1, "danish@example.com", false)
        );

        assertThat(response.getTitle()).isEqualTo("Updated title");
        assertThat(response.getContent()).isEqualTo("Updated content");
    }

    @Test
    void deletePostShouldAllowAdmin() {
        when(postRepo.findById(10)).thenReturn(Optional.of(post));

        postService.deletePost(10, new AuthenticatedUser(99, "admin@example.com", true));

        verify(postRepo).delete(post);
    }

    @Test
    void getPostsByUserShouldQueryScalarAuthorId() {
        when(postRepo.findByAuthorId(1)).thenReturn(List.of(post));

        List<PostDto> result = postService.getPostByUser(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthorId()).isEqualTo(1);
        verify(postRepo).findByAuthorId(1);
    }

    private PostCreateRequest createRequest() {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("Secure services");
        request.setContent("Service boundary content");
        request.setCategoryId(3);
        return request;
    }

    private PostUpdateRequest updateRequest() {
        PostUpdateRequest request = new PostUpdateRequest();
        request.setTitle("Updated title");
        request.setContent("Updated content");
        return request;
    }
}
