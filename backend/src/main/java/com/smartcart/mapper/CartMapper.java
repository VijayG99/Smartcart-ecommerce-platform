package com.smartcart.mapper;

import com.smartcart.dto.CartItemResponse;
import com.smartcart.dto.CartResponse;
import com.smartcart.entity.Cart;
import com.smartcart.entity.CartItem;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CartMapper {
    private CartMapper() {}

    /** @param stock map of productId -> available quantity (loaded with one query by the caller) */
    public static CartResponse toResponse(Cart cart, Map<Long, Integer> stock) {
        List<CartItemResponse> items = cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getId))
                .map(i -> toItem(i, stock.getOrDefault(i.getProduct().getId(), 0)))
                .toList();
        BigDecimal total = items.stream().map(CartItemResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = items.stream().mapToInt(CartItemResponse::quantity).sum();
        return new CartResponse(cart.getId(), items, count, total);
    }

    private static CartItemResponse toItem(CartItem i, int available) {
        BigDecimal unit = i.getProduct().getPrice();
        return new CartItemResponse(i.getId(), i.getProduct().getId(), i.getProduct().getName(), unit,
                i.getQuantity(), unit.multiply(BigDecimal.valueOf(i.getQuantity())), available);
    }
}
