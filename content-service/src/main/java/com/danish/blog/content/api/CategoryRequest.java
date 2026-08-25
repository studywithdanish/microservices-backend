package com.danish.blog.content.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category title is required")
        @Size(min = 4, max = 255, message = "Category title must contain 4-255 characters")
        String categoryTitle,
        @NotBlank(message = "Category description is required")
        @Size(min = 10, max = 255, message = "Category description must contain 10-255 characters")
        String categoryDescription
) {
}
