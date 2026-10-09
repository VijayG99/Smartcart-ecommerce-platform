package com.smartcart.service;

import com.smartcart.dto.InventoryResponse;
import com.smartcart.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {
    InventoryResponse getByProductId(Long productId);
    Page<InventoryResponse> findAll(Pageable pageable);
    InventoryResponse updateStock(Long productId, int quantity);
    void createFor(Product product, int quantity);
    int getAvailable(Long productId);
    /** Locks the stock row and subtracts. Throws InsufficientStockException (and rolls back the caller) if not enough. */
    void reduceStock(Long productId, int quantity);
    void restoreStock(Long productId, int quantity);
    void warnIfLowStock(Long productId);
}
