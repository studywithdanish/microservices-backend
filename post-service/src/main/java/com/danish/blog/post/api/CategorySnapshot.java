package com.danish.blog.post.api;

public record CategorySnapshot(
        Integer categoryId,
        String categoryTitle,
        String categoryDescription
) {
}
