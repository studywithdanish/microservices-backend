package com.danish.blog.services.impl;

import com.danish.blog.entities.Category;
import com.danish.blog.exceptions.ResourceNotFoundException;
import com.danish.blog.payloads.CategoryDto;
import com.danish.blog.repositories.CategoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepo categoryRepo;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setCategoryId(1);
        category.setCategoryTitle("Java");
        category.setCategoryDescription("Java backend articles");

        categoryDto = new CategoryDto();
        categoryDto.setCategoryId(1);
        categoryDto.setCategoryTitle("Java");
        categoryDto.setCategoryDescription("Java backend articles");
    }

    @Test
    void createCategoryShouldSaveAndReturnCategory() {
        when(modelMapper.map(categoryDto, Category.class)).thenReturn(category);
        when(categoryRepo.save(category)).thenReturn(category);
        when(modelMapper.map(category, CategoryDto.class)).thenReturn(categoryDto);

        CategoryDto result = categoryService.createCategory(categoryDto);

        assertThat(result.getCategoryId()).isEqualTo(1);
        assertThat(result.getCategoryTitle()).isEqualTo("Java");
        verify(categoryRepo).save(category);
    }

    @Test
    void updateCategoryShouldUpdateExistingCategory() {
        CategoryDto updateRequest = new CategoryDto();
        updateRequest.setCategoryTitle("Spring Boot");
        updateRequest.setCategoryDescription("Spring Boot backend guides");

        CategoryDto updatedDto = new CategoryDto();
        updatedDto.setCategoryId(1);
        updatedDto.setCategoryTitle("Spring Boot");
        updatedDto.setCategoryDescription("Spring Boot backend guides");

        when(categoryRepo.findById(1)).thenReturn(Optional.of(category));
        when(categoryRepo.save(category)).thenReturn(category);
        when(modelMapper.map(category, CategoryDto.class)).thenReturn(updatedDto);

        CategoryDto result = categoryService.updateCategory(updateRequest, 1);

        assertThat(result.getCategoryTitle()).isEqualTo("Spring Boot");
        assertThat(category.getCategoryDescription()).isEqualTo("Spring Boot backend guides");
        verify(categoryRepo).save(category);
    }

    @Test
    void getCategoryShouldThrowWhenCategoryDoesNotExist() {
        when(categoryRepo.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void getAllCategoriesShouldReturnMappedCategories() {
        when(categoryRepo.findAll()).thenReturn(List.of(category));
        when(modelMapper.map(any(Category.class), any())).thenReturn(categoryDto);

        List<CategoryDto> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryTitle()).isEqualTo("Java");
    }

    @Test
    void deleteCategoryShouldDeleteExistingCategory() {
        when(categoryRepo.findById(1)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1);

        verify(categoryRepo).delete(category);
    }
}
