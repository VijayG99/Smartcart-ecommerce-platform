package com.smartcart.dto;

public record InventoryResponse(Long productId, String productName, String sku, int quantity) {
}
