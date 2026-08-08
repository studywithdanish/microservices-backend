package com.danish.blog.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class PostCreateRequest {

    @NotBlank(message = "Post title is required")
    @Size(max = 100, message = "Post title must not exceed 100 characters")
    private String title;

    @NotBlank(message = "Post content is required")
    @Size(max = 10000, message = "Post content must not exceed 10000 characters")
    private String content;

    private Integer categoryId;
}
