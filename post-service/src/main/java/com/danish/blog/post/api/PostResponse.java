package com.danish.blog.post.api;

import java.util.List;

public record PostResponse(
        List<PostDto> content,
        Integer pageNo,
        Integer pageSize,
        Long totalElement,
        Integer totalPages,
        Boolean lastPage
) {
}
