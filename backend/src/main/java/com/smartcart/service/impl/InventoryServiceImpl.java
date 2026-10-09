package com.smartcart.service.impl;

import com.smartcart.cache.ProductCache;
import com.smartcart.dto.InventoryResponse;
import com.smartcart.entity.Inventory;
import com.smartcart.entity.Product;
import com.smartcart.exception.InsufficientStockException;
import com.smartcart.exception.ResourceNotFoundException;
import com.smartcart.mapper.ProductMapper;
import com.smartcart.repository.InventoryRepository;
import com.smartcart.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ProductCache productCache;

    @Value("${smartcart.inventory.low-stock-threshold:5}")
    private int lowStockThreshold;

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId).map(ProductMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product " + productId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> findAll(Pageable pageable) {
        return inventoryRepository.findAllWithProduct(pageable).map(ProductMapper::toResponse);
    }

    @Override
    @Transactional
    public InventoryResponse updateStock(Long productId, int quantity) {
        Inventory inv = lockedInventory(productId);
        inv.setQuantity(quantity);
        productCache.evictAfterCommit(productId);
        log.info("Stock for product {} set to {}", productId, quantity);
        return ProductMapper.toResponse(inv);
    }

    @Override
    @Transactional
    public void createFor(Product product, int quantity) {
        inventoryRepository.save(Inventory.builder().product(product).quantity(quantity).build());
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailable(Long productId) {
        return inventoryRepository.findByProductId(productId).map(Inventory::getQuantity).orElse(0);
    }

    /**
     * Joins the caller's transaction (default REQUIRED). The row is locked with SELECT ... FOR UPDATE, so two
     * customers buying the last unit cannot both succeed. If this throws, the whole order transaction rolls back.
     */
    @Override
    @Transactional
    public void reduceStock(Long productId, int quantity) {
        Inventory inv = lockedInventory(productId);
        if (inv.getQuantity() < quantity) {
            throw new InsufficientStockException("Insufficient stock for product " + productId
                    + ": requested " + quantity + ", available " + inv.getQuantity());
        }
        inv.setQuantity(inv.getQuantity() - quantity);
        productCache.evictAfterCommit(productId);
    }

    @Override
    @Transactional
    public void restoreStock(Long productId, int quantity) {
        Inventory inv = lockedInventory(productId);
        inv.setQuantity(inv.getQuantity() + quantity);
        productCache.evictAfterCommit(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public void warnIfLowStock(Long productId) {
        inventoryRepository.findByProductId(productId).ifPresent(inv -> {
            if (inv.getQuantity() <= lowStockThreshold) {
                log.warn("LOW STOCK: product {} has only {} units left (threshold {})",
                        productId, inv.getQuantity(), lowStockThreshold);
            }
        });
    }

    private Inventory lockedInventory(Long productId) {
        return inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product " + productId));
    }
}
