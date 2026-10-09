package com.smartcart.controller;

import com.smartcart.config.OpenApiConfig;
import com.smartcart.dto.CategoryRequest;
import com.smartcart.dto.CategoryResponse;
import com.smartcart.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categories")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(summary = "List all categories (public)")
    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.findAll();
    }

    @Operation(summary = "Create a category (ADMIN)")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @Operation(summary = "Update a category (ADMIN)")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @Operation(summary = "Delete an empty category (ADMIN)")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
