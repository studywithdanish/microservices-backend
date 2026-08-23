package com.danish.blog.services.impl;

import com.danish.blog.clients.PostReferenceClient;
import com.danish.blog.entities.Comment;
import com.danish.blog.exceptions.ResourceNotFoundException;
import com.danish.blog.payloads.CommentCreateRequest;
import com.danish.blog.payloads.CommentDto;
import com.danish.blog.repositories.CommentRepo;
import com.danish.blog.security.AuthenticatedUser;
import com.danish.blog.services.CommentService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepo commentRepo;
    private final PostReferenceClient postReferenceClient;

    public CommentServiceImpl(CommentRepo commentRepo, PostReferenceClient postReferenceClient) {
        this.commentRepo = commentRepo;
        this.postReferenceClient = postReferenceClient;
    }

    @Override
    public CommentDto createComment(CommentCreateRequest request, Integer postId, AuthenticatedUser actor) {
        requireAuthenticatedActor(actor);
        if (!postReferenceClient.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "PostId", postId);
        }

        Comment comment = new Comment();
        comment.setContent(request.getContent().trim());
        comment.setPostId(postId);
        comment.setAuthorId(actor.id());
        return toDto(commentRepo.save(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByPost(Integer postId) {
        if (!postReferenceClient.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "PostId", postId);
        }
        return commentRepo.findByPostIdOrderByIdAsc(postId).stream().map(this::toDto).toList();
    }

    @Override
    public void deleteComment(Integer commentId, AuthenticatedUser actor) {
        requireAuthenticatedActor(actor);
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "Comment Id", commentId));
        if (!actor.canManage(comment.getAuthorId())) {
            throw new AccessDeniedException("You cannot delete another user's comment");
        }
        commentRepo.delete(comment);
    }

    private void requireAuthenticatedActor(AuthenticatedUser actor) {
        if (actor == null) {
            throw new AccessDeniedException("Authentication is required");
        }
    }

    private CommentDto toDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setPostId(comment.getPostId());
        dto.setAuthorId(comment.getAuthorId());
        return dto;
    }
}
