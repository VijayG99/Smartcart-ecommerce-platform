package com.smartcart.dto;

import com.smartcart.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, Long userId, OrderStatus status, BigDecimal totalAmount,
                            String shippingAddress, List<OrderItemResponse> items,
                            Instant createdAt, Instant updatedAt) {
}
