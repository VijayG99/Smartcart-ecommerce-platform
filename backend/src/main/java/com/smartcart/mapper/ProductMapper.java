package com.smartcart.mapper;

import com.smartcart.dto.CategoryResponse;
import com.smartcart.dto.InventoryResponse;
import com.smartcart.dto.ProductResponse;
import com.smartcart.entity.Category;
import com.smartcart.entity.Inventory;
import com.smartcart.entity.Product;

public final class ProductMapper {
    private ProductMapper() {}

    public static ProductResponse toResponse(Product p, int availableQuantity) {
        return ProductResponse.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .price(p.getPrice()).sku(p.getSku())
                .categoryId(p.getCategory().getId()).categoryName(p.getCategory().getName())
                .availableQuantity(availableQuantity).active(p.isActive())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }

    public static CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }

    public static InventoryResponse toResponse(Inventory i) {
        return new InventoryResponse(i.getProduct().getId(), i.getProduct().getName(),
                i.getProduct().getSku(), i.getQuantity());
    }
}
