package com.danish.blog.post.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotBlank(message = "Post title is required")
        @Size(max = 100, message = "Post title must not exceed 100 characters")
        String title,
        @NotBlank(message = "Post content is required")
        @Size(max = 10000, message = "Post content must not exceed 10000 characters")
        String content,
        Integer categoryId
) {
}
