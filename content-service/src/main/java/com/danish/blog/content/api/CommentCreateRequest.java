package com.danish.blog.content.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank(message = "Comment content is required")
        @Size(max = 255, message = "Comment content must not exceed 255 characters")
        String content
) {
}
