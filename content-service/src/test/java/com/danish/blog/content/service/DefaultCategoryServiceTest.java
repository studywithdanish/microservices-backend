package com.danish.blog.content.service;

import com.danish.blog.content.api.CategoryDto;
import com.danish.blog.content.api.CategoryRequest;
import com.danish.blog.content.domain.Category;
import com.danish.blog.content.error.ResourceNotFoundException;
import com.danish.blog.content.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private DefaultCategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new DefaultCategoryService(categoryRepository);
    }

    @Test
    void createsTrimmedCategory() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setCategoryId(1);
            return category;
        });

        CategoryDto result = categoryService.create(new CategoryRequest(
                "  Java  ",
                "  Java backend articles  "
        ));

        assertThat(result.categoryId()).isEqualTo(1);
        assertThat(result.categoryTitle()).isEqualTo("Java");
        assertThat(result.categoryDescription()).isEqualTo("Java backend articles");
    }

    @Test
    void updatesExistingCategory() {
        Category category = category(1, "Java", "Java backend articles");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryDto result = categoryService.update(
                new CategoryRequest("Spring Boot", "Spring Boot backend guides"),
                1
        );

        assertThat(result.categoryTitle()).isEqualTo("Spring Boot");
        verify(categoryRepository).save(category);
    }

    @Test
    void returnsCategoriesInStableIdOrder() {
        when(categoryRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(category(1, "Java", "Java backend articles")));

        List<CategoryDto> result = categoryService.getAll();

        assertThat(result).extracting(CategoryDto::categoryTitle).containsExactly("Java");
    }

    @Test
    void deletesExistingCategory() {
        Category category = category(1, "Java", "Java backend articles");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        categoryService.delete(1);

        verify(categoryRepository).delete(category);
    }

    @Test
    void rejectsMissingCategory() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    private Category category(Integer id, String title, String description) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryTitle(title);
        category.setCategoryDescription(description);
        return category;
    }
}
