package com.smartcart.repository;

import com.smartcart.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByIdAndActiveTrue(Long id);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
    boolean existsByCategoryId(Long categoryId);
}
