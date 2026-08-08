package com.danish.blog.services.impl;

import com.danish.blog.entities.Comment;
import com.danish.blog.exceptions.ResourceNotFoundException;
import com.danish.blog.payloads.CommentCreateRequest;
import com.danish.blog.payloads.CommentDto;
import com.danish.blog.repositories.CommentRepo;
import com.danish.blog.security.AuthenticatedUser;
import com.danish.blog.services.PostService;
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
class CommentServiceImplTest {

    @Mock
    private CommentRepo commentRepo;

    @Mock
    private PostService postService;

    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(commentRepo, postService);
    }

    @Test
    void createCommentShouldStorePostAndAuthenticatedAuthorIds() {
        CommentCreateRequest request = request();
        when(postService.existsById(10)).thenReturn(true);
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(7);
            return saved;
        });

        CommentDto response = commentService.createComment(
                request,
                10,
                new AuthenticatedUser(1, "danish@example.com", false)
        );

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepo).save(captor.capture());
        assertThat(captor.getValue().getPostId()).isEqualTo(10);
        assertThat(captor.getValue().getAuthorId()).isEqualTo(1);
        assertThat(response.getAuthorId()).isEqualTo(1);
    }

    @Test
    void createCommentShouldRejectMissingPost() {
        when(postService.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> commentService.createComment(
                request(),
                99,
                new AuthenticatedUser(1, "danish@example.com", false)
        )).isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepo, never()).save(any());
    }

    @Test
    void deleteCommentShouldRejectDifferentNonAdminUser() {
        Comment comment = comment(7, 10, 1);
        when(commentRepo.findById(7)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(
                7,
                new AuthenticatedUser(2, "other@example.com", false)
        )).isInstanceOf(AccessDeniedException.class);

        verify(commentRepo, never()).delete(any());
    }

    @Test
    void deleteLegacyCommentShouldAllowAdmin() {
        Comment comment = comment(7, 10, null);
        when(commentRepo.findById(7)).thenReturn(Optional.of(comment));

        commentService.deleteComment(7, new AuthenticatedUser(99, "admin@example.com", true));

        verify(commentRepo).delete(comment);
    }

    @Test
    void getCommentsShouldReturnPostCommentsInRepositoryOrder() {
        when(postService.existsById(10)).thenReturn(true);
        when(commentRepo.findByPostIdOrderByIdAsc(10)).thenReturn(List.of(comment(7, 10, 1)));

        List<CommentDto> result = commentService.getCommentsByPost(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPostId()).isEqualTo(10);
    }

    private CommentCreateRequest request() {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("Useful explanation");
        return request;
    }

    private Comment comment(Integer id, Integer postId, Integer authorId) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setContent("Useful explanation");
        return comment;
    }
}
