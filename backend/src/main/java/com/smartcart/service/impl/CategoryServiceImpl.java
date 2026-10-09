package com.smartcart.service.impl;

import com.smartcart.dto.CategoryRequest;
import com.smartcart.dto.CategoryResponse;
import com.smartcart.entity.Category;
import com.smartcart.exception.ConflictException;
import com.smartcart.exception.DuplicateResourceException;
import com.smartcart.exception.ResourceNotFoundException;
import com.smartcart.mapper.ProductMapper;
import com.smartcart.repository.CategoryRepository;
import com.smartcart.repository.ProductRepository;
import com.smartcart.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(ProductMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest r) {
        if (categoryRepository.existsByName(r.name().trim())) {
            throw new DuplicateResourceException("Category already exists: " + r.name());
        }
        Category saved = categoryRepository.save(
                Category.builder().name(r.name().trim()).description(r.description()).build());
        return ProductMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest r) {
        Category c = find(id);
        if (categoryRepository.existsByNameAndIdNot(r.name().trim(), id)) {
            throw new DuplicateResourceException("Category already exists: " + r.name());
        }
        c.setName(r.name().trim());
        c.setDescription(r.description());
        return ProductMapper.toResponse(c);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category c = find(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category still has products and cannot be deleted");
        }
        categoryRepository.delete(c);
    }

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
