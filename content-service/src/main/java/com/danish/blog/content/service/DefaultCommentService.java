package com.danish.blog.content.service;

import com.danish.blog.content.api.CommentCreateRequest;
import com.danish.blog.content.api.CommentDto;
import com.danish.blog.content.client.PostReferenceClient;
import com.danish.blog.content.domain.Comment;
import com.danish.blog.content.error.ResourceNotFoundException;
import com.danish.blog.content.observability.ContentMetrics;
import com.danish.blog.content.repository.CommentRepository;
import com.danish.blog.content.security.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DefaultCommentService implements CommentService {

    private final CommentRepository commentRepository;
    private final PostReferenceClient postReferenceClient;
    private final ContentMetrics metrics;

    public DefaultCommentService(
            CommentRepository commentRepository,
            PostReferenceClient postReferenceClient,
            ContentMetrics metrics
    ) {
        this.commentRepository = commentRepository;
        this.postReferenceClient = postReferenceClient;
        this.metrics = metrics;
    }

    @Override
    public CommentDto create(CommentCreateRequest request, Integer postId, AuthenticatedUser actor) {
        requirePost(postId);
        Comment comment = new Comment();
        comment.setContent(request.content().trim());
        comment.setPostId(postId);
        comment.setAuthorId(actor.id());
        Comment saved = commentRepository.save(comment);
        metrics.recordCommentCreated();
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getByPost(Integer postId) {
        requirePost(postId);
        return commentRepository.findByPostIdOrderByIdAsc(postId).stream().map(this::toDto).toList();
    }

    @Override
    public void delete(Integer commentId, AuthenticatedUser actor) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "commentId", commentId));
        if (!actor.canManage(comment.getAuthorId())) {
            throw new AccessDeniedException("You cannot delete another user's comment");
        }
        commentRepository.delete(comment);
        metrics.recordCommentDeleted();
    }

    private void requirePost(Integer postId) {
        if (!postReferenceClient.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "postId", postId);
        }
    }

    private CommentDto toDto(Comment comment) {
        return new CommentDto(comment.getId(), comment.getContent(), comment.getPostId(), comment.getAuthorId());
    }
}
