package com.danish.blog.content.api;

public record CategoryDto(
        Integer categoryId,
        String categoryTitle,
        String categoryDescription
) {
}
