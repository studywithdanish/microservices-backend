package com.danish.blog.content.service;

import com.danish.blog.content.api.CommentCreateRequest;
import com.danish.blog.content.api.CommentDto;
import com.danish.blog.content.client.PostReferenceClient;
import com.danish.blog.content.domain.Comment;
import com.danish.blog.content.error.ResourceNotFoundException;
import com.danish.blog.content.repository.CommentRepository;
import com.danish.blog.content.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class DefaultCommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostReferenceClient postReferenceClient;

    private DefaultCommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new DefaultCommentService(commentRepository, postReferenceClient);
    }

    @Test
    void createsCommentWithAuthenticatedAuthor() {
        when(postReferenceClient.existsById(10)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(7);
            return comment;
        });

        CommentDto result = commentService.create(
                new CommentCreateRequest("  Useful explanation  "),
                10,
                new AuthenticatedUser(1, "danish@example.com", false)
        );

        assertThat(result.id()).isEqualTo(7);
        assertThat(result.content()).isEqualTo("Useful explanation");
        assertThat(result.authorId()).isEqualTo(1);
    }

    @Test
    void rejectsMissingPost() {
        when(postReferenceClient.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> commentService.create(
                new CommentCreateRequest("Useful explanation"),
                99,
                new AuthenticatedUser(1, "danish@example.com", false)
        )).isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void returnsPostCommentsInRepositoryOrder() {
        when(postReferenceClient.existsById(10)).thenReturn(true);
        when(commentRepository.findByPostIdOrderByIdAsc(10)).thenReturn(List.of(comment(7, 10, 1)));

        List<CommentDto> result = commentService.getByPost(10);

        assertThat(result).extracting(CommentDto::id).containsExactly(7);
    }

    @Test
    void ownerCanDeleteComment() {
        Comment comment = comment(7, 10, 1);
        when(commentRepository.findById(7)).thenReturn(Optional.of(comment));

        commentService.delete(7, new AuthenticatedUser(1, "danish@example.com", false));

        verify(commentRepository).delete(comment);
    }

    @Test
    void differentNormalUserCannotDeleteComment() {
        Comment comment = comment(7, 10, 1);
        when(commentRepository.findById(7)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(
                7,
                new AuthenticatedUser(2, "other@example.com", false)
        )).isInstanceOf(AccessDeniedException.class);

        verify(commentRepository, never()).delete(any());
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
