package com.danish.blog.content.service;

import com.danish.blog.content.api.CategoryDto;
import com.danish.blog.content.api.CategoryRequest;

import java.util.List;

public interface CategoryService {

    CategoryDto create(CategoryRequest request);

    CategoryDto update(CategoryRequest request, Integer categoryId);

    void delete(Integer categoryId);

    CategoryDto getById(Integer categoryId);

    List<CategoryDto> getAll();
}
