package com.danish.blog.payloads;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class PostResponse {

    private List<PostDto> content;
    private Integer pageNo;
    private Integer pageSize;
    private Long totalElement;
    private Integer totalPages;
    private Boolean lastPage;
}
