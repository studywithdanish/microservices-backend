package com.danish.blog.controllers;

import com.danish.blog.exceptions.GlobalExceptionHandler;
import com.danish.blog.exceptions.ResourceNotFoundException;
import com.danish.blog.payloads.CategoryDto;
import com.danish.blog.repositories.RoleRepo;
import com.danish.blog.security.JwtTokenHelper;
import com.danish.blog.services.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private RoleRepo roleRepo;

    @MockitoBean
    private JwtTokenHelper jwtTokenHelper;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getAllCategoriesShouldReturnCategories() throws Exception {
        CategoryDto category = categoryDto(1, "Java", "Java backend articles");
        when(categoryService.getAllCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/categories/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].categoryTitle").value("Java"));
    }

    @Test
    void getCategoryShouldReturnCategory() throws Exception {
        when(categoryService.getCategory(1)).thenReturn(categoryDto(1, "Spring", "Spring Boot articles"));

        mockMvc.perform(get("/api/categories/{categoryId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryTitle").value("Spring"));
    }

    @Test
    void getCategoryShouldReturnNotFoundWhenMissing() throws Exception {
        when(categoryService.getCategory(99))
                .thenThrow(new ResourceNotFoundException("Category", "Category Id", 99));

        mockMvc.perform(get("/api/categories/{categoryId}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category not found with Category Id : 99"));
    }

    @Test
    void createCategoryShouldReturnCreatedCategory() throws Exception {
        CategoryDto request = categoryDto(null, "Docker", "Docker platform articles");
        CategoryDto response = categoryDto(2, "Docker", "Docker platform articles");
        when(categoryService.createCategory(any(CategoryDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(2))
                .andExpect(jsonPath("$.categoryTitle").value("Docker"));
    }

    @Test
    void createCategoryShouldReturnBadRequestForInvalidPayload() throws Exception {
        CategoryDto request = categoryDto(null, "API", "short");

        mockMvc.perform(post("/api/categories/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.categoryTitle").value("Min size of Category title is 4"))
                .andExpect(jsonPath("$.categoryDescription").exists());
    }

    @Test
    void deleteCategoryShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(delete("/api/categories/{categoryId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Category is deleted Successfully !!"));

        verify(categoryService).deleteCategory(1);
    }

    private CategoryDto categoryDto(Integer id, String title, String description) {
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setCategoryId(id);
        categoryDto.setCategoryTitle(title);
        categoryDto.setCategoryDescription(description);
        return categoryDto;
    }
}
