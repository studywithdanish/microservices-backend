package com.danish.blog.content.api;

public record CommentDto(
        Integer id,
        String content,
        Integer postId,
        Integer authorId
) {
}
