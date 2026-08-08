package com.danish.blog.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class CommentCreateRequest {

    @NotBlank(message = "Comment content is required")
    @Size(max = 255, message = "Comment content must not exceed 255 characters")
    private String content;
}
