package com.smartcart.service;

import com.smartcart.dto.OrderRequest;
import com.smartcart.dto.OrderResponse;
import com.smartcart.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse placeOrder(Long userId, OrderRequest request);
    OrderResponse getById(Long orderId, Long callerId, boolean callerIsAdmin);
    Page<OrderResponse> getByUser(Long userId, Long callerId, boolean callerIsAdmin, Pageable pageable);
    Page<OrderResponse> getAll(Pageable pageable);
    OrderResponse updateStatus(Long orderId, OrderStatus newStatus);
    /** Called by the inventory Kafka consumer: PENDING -> CONFIRMED (idempotent). */
    void confirmOrder(Long orderId);
}
