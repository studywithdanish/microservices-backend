package com.danish.blog.content.api;

import com.danish.blog.content.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> update(
            @Valid @RequestBody CategoryRequest request,
            @PathVariable Integer categoryId
    ) {
        return ResponseEntity.ok(categoryService.update(request, categoryId));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Integer categoryId) {
        categoryService.delete(categoryId);
        return ResponseEntity.ok(new ApiResponse("Category deleted successfully", true));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> getById(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(categoryService.getById(categoryId));
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<CategoryDto>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }
}
