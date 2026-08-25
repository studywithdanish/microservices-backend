package com.danish.blog.content.service;

import com.danish.blog.content.api.CategoryDto;
import com.danish.blog.content.api.CategoryRequest;
import com.danish.blog.content.domain.Category;
import com.danish.blog.content.error.ResourceNotFoundException;
import com.danish.blog.content.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DefaultCategoryService implements CategoryService {

    private final CategoryRepository categoryRepository;

    public DefaultCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CategoryDto create(CategoryRequest request) {
        Category category = new Category();
        apply(category, request);
        return toDto(categoryRepository.save(category));
    }

    @Override
    public CategoryDto update(CategoryRequest request, Integer categoryId) {
        Category category = findCategory(categoryId);
        apply(category, request);
        return toDto(categoryRepository.save(category));
    }

    @Override
    public void delete(Integer categoryId) {
        categoryRepository.delete(findCategory(categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getById(Integer categoryId) {
        return toDto(findCategory(categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryId"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private Category findCategory(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));
    }

    private void apply(Category category, CategoryRequest request) {
        category.setCategoryTitle(request.categoryTitle().trim());
        category.setCategoryDescription(request.categoryDescription().trim());
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getCategoryId(),
                category.getCategoryTitle(),
                category.getCategoryDescription()
        );
    }
}
