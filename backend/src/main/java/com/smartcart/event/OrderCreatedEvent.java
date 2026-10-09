package com.smartcart.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Published after an order is committed. It carries everything consumers need,
 * so they do not have to call back into the order module (loose coupling).
 */
public record OrderCreatedEvent(Long orderId, Long userId, String customerEmail, BigDecimal totalAmount,
                                List<Item> items, Instant createdAt) {

    public record Item(Long productId, String productName, int quantity, BigDecimal unitPrice) {
    }
}
