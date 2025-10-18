package com.danish.blog.payloads;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;

@NoArgsConstructor
@Getter
@Setter
public class CategoryDto {

    private Integer categoryId;

    @NotBlank
    @Size(min = 4, message = "Min size of Category title is 4")
    private String categoryTitle;

    @NotBlank
    @Size(min = 10)
    private String categoryDescription;
}
