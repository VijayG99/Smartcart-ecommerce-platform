package com.smartcart.service;

import com.smartcart.dto.CartItemRequest;
import com.smartcart.dto.CartResponse;
import com.smartcart.dto.QuantityRequest;

public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addItem(Long userId, CartItemRequest request);
    CartResponse updateItem(Long userId, Long itemId, QuantityRequest request);
    CartResponse removeItem(Long userId, Long itemId);
}
