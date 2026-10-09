package com.smartcart.service;

import com.smartcart.dto.ProductRequest;
import com.smartcart.dto.ProductResponse;
import com.smartcart.dto.ProductSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse create(ProductRequest request);
    ProductResponse getById(Long id);
    ProductResponse update(Long id, ProductRequest request);
    void delete(Long id);
    Page<ProductResponse> search(ProductSearchCriteria criteria, Pageable pageable);
}
