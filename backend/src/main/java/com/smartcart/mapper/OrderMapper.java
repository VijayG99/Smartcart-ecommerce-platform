package com.smartcart.mapper;

import com.smartcart.dto.OrderItemResponse;
import com.smartcart.dto.OrderResponse;
import com.smartcart.entity.Order;
import com.smartcart.entity.OrderItem;

import java.math.BigDecimal;

public final class OrderMapper {
    private OrderMapper() {}

    public static OrderResponse toResponse(Order o) {
        return new OrderResponse(o.getId(), o.getUser().getId(), o.getStatus(), o.getTotalAmount(),
                o.getShippingAddress(), o.getItems().stream().map(OrderMapper::toItem).toList(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private static OrderItemResponse toItem(OrderItem i) {
        return new OrderItemResponse(i.getProduct().getId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
    }
}
