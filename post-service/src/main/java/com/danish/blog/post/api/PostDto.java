package com.danish.blog.post.api;

import java.util.Date;

public record PostDto(
        Integer postId,
        String title,
        String content,
        String imageName,
        Date addedDate,
        CategorySnapshot category,
        Integer authorId
) {
}
