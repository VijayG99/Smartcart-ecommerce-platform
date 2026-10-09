package com.smartcart.service.impl;

import com.smartcart.dto.CartItemRequest;
import com.smartcart.dto.CartResponse;
import com.smartcart.dto.QuantityRequest;
import com.smartcart.entity.Cart;
import com.smartcart.entity.CartItem;
import com.smartcart.entity.Inventory;
import com.smartcart.entity.Product;
import com.smartcart.exception.InsufficientStockException;
import com.smartcart.exception.ResourceNotFoundException;
import com.smartcart.mapper.CartMapper;
import com.smartcart.repository.CartRepository;
import com.smartcart.repository.InventoryRepository;
import com.smartcart.repository.ProductRepository;
import com.smartcart.repository.UserRepository;
import com.smartcart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/** Every method takes the userId from the JWT principal, so a customer can only ever touch their own cart. */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CartResponse getCart(Long userId) {
        return toResponse(getOrCreateCart(userId));
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Product product = productRepository.findByIdAndActiveTrue(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
        Cart cart = getOrCreateCart(userId);

        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst().orElse(null);

        // Duplicate product: merge into the existing line instead of creating a second one.
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        ensureStock(product.getId(), newQuantity);

        if (existing == null) {
            cart.getItems().add(CartItem.builder().cart(cart).product(product).quantity(newQuantity).build());
        } else {
            existing.setQuantity(newQuantity);
        }
        return toResponse(cartRepository.saveAndFlush(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long itemId, QuantityRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findItem(cart, itemId);
        ensureStock(item.getProduct().getId(), request.quantity());
        item.setQuantity(request.quantity());
        return toResponse(cartRepository.saveAndFlush(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findItem(cart, itemId);
        cart.getItems().remove(item);   // orphanRemoval deletes the row
        return toResponse(cartRepository.saveAndFlush(cart));
    }

    private CartItem findItem(Cart cart, Long itemId) {
        return cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() ->
                cartRepository.save(Cart.builder().user(userRepository.getReferenceById(userId)).build()));
    }

    private void ensureStock(Long productId, int wanted) {
        int available = inventoryRepository.findByProductId(productId).map(Inventory::getQuantity).orElse(0);
        if (wanted > available) {
            throw new InsufficientStockException("Only " + available + " unit(s) available for this product");
        }
    }

    private CartResponse toResponse(Cart cart) {
        var ids = cart.getItems().stream().map(i -> i.getProduct().getId()).toList();
        Map<Long, Integer> stock = ids.isEmpty() ? Map.of()
                : inventoryRepository.findByProductIdIn(ids).stream()
                .collect(Collectors.toMap(i -> i.getProduct().getId(), Inventory::getQuantity));
        return CartMapper.toResponse(cart, stock);
    }
}
