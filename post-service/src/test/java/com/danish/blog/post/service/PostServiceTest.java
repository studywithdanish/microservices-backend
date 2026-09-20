package com.danish.blog.post.service;

import com.danish.blog.post.api.CategorySnapshot;
import com.danish.blog.post.api.PostCreateRequest;
import com.danish.blog.post.api.PostDto;
import com.danish.blog.post.api.PostUpdateRequest;
import com.danish.blog.post.client.CategoryClient;
import com.danish.blog.post.domain.Post;
import com.danish.blog.post.error.ApiException;
import com.danish.blog.post.repository.PostRepository;
import com.danish.blog.post.security.JwtPrincipal;
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
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CategoryClient categoryClient;

    @Mock
    private PostEventOutbox postEventOutbox;

    private PostService postService;
    private Post post;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, categoryClient, postEventOutbox);
        post = post(10, 1);
    }

    @Test
    void createStoresAuthenticatedAuthorAndCategorySnapshot() {
        when(categoryClient.getCategory(3)).thenReturn(
                new CategorySnapshot(3, "Spring", "Spring Boot articles")
        );
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            saved.setId(10);
            return saved;
        });

        PostDto result = postService.create(
                new PostCreateRequest("Secure services", "Boundary content", 3),
                1
        );

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        verify(postEventOutbox).recordPostPublished(captor.getValue());
        assertThat(captor.getValue().getAuthorId()).isEqualTo(1);
        assertThat(captor.getValue().getCategoryTitle()).isEqualTo("Spring");
        assertThat(result.category().categoryId()).isEqualTo(3);
    }

    @Test
    void createRejectsMissingCategoryIdBeforeCallingAnotherService() {
        assertThatThrownBy(() -> postService.create(
                new PostCreateRequest("Title", "Content", null),
                1
        )).isInstanceOf(ApiException.class);

        verify(categoryClient, never()).getCategory(any());
    }

    @Test
    void updateRejectsDifferentNonAdminUser() {
        when(postRepository.findById(10)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.update(
                new PostUpdateRequest("Updated", "Updated content"),
                10,
                principal(2, "ROLE_NORMAL")
        )).isInstanceOf(AccessDeniedException.class);

        verify(postRepository, never()).save(any());
    }

    @Test
    void deleteAllowsAdministrator() {
        when(postRepository.findById(10)).thenReturn(Optional.of(post));

        postService.delete(10, principal(99, "ROLE_ADMIN"));

        verify(postRepository).delete(post);
    }

    @Test
    void categoryQueryValidatesCategoryAndUsesScalarIdentifier() {
        when(categoryClient.getCategory(3)).thenReturn(new CategorySnapshot(3, "Spring", "Articles"));
        when(postRepository.findByCategoryId(3)).thenReturn(List.of(post));

        List<PostDto> result = postService.getByCategory(3);

        assertThat(result).hasSize(1);
        verify(categoryClient).getCategory(3);
        verify(postRepository).findByCategoryId(3);
    }

    private Post post(Integer id, Integer authorId) {
        Post value = new Post();
        value.setId(id);
        value.setTitle("Secure services");
        value.setContent("Boundary content");
        value.setImageName("default.png");
        value.setAddedDate(new java.util.Date());
        value.setAuthorId(authorId);
        value.setCategoryId(3);
        value.setCategoryTitle("Spring");
        value.setCategoryDescription("Articles");
        return value;
    }

    private JwtPrincipal principal(Integer id, String role) {
        return new JwtPrincipal(id, "user@example.com", List.of(role));
    }
}
