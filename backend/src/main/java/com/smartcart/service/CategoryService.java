package com.smartcart.service;

import com.smartcart.dto.CategoryRequest;
import com.smartcart.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> findAll();
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Long id, CategoryRequest request);
    void delete(Long id);
}
