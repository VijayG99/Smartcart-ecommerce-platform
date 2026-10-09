package com.smartcart.dto;

import java.math.BigDecimal;

public record CartItemResponse(Long id, Long productId, String productName, BigDecimal unitPrice,
                               int quantity, BigDecimal lineTotal, int availableQuantity) {
}
