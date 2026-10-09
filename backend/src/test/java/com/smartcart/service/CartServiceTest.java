package com.smartcart.service;

import com.smartcart.dto.CartItemRequest;
import com.smartcart.dto.CartResponse;
import com.smartcart.entity.*;
import com.smartcart.exception.InsufficientStockException;
import com.smartcart.repository.*;
import com.smartcart.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock CartRepository cartRepository;
    @Mock ProductRepository productRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock UserRepository userRepository;
    @InjectMocks CartServiceImpl cartService;

    Product product;
    Cart cart;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(1L).name("Lamp").price(new BigDecimal("10.00")).build();
        cart = Cart.builder().id(1L).build();
        CartItem existing = CartItem.builder().id(100L).cart(cart).product(product).quantity(2).build();
        cart.getItems().add(existing);
        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
    }

    @Test
    void addingSameProductMergesQuantityAndCalculatesTotal() {
        Inventory inventory = Inventory.builder().product(product).quantity(10).build();
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.findByProductIdIn(any())).thenReturn(List.of(inventory));
        when(cartRepository.saveAndFlush(cart)).thenReturn(cart);

        CartResponse response = cartService.addItem(5L, new CartItemRequest(1L, 3));

        assertEquals(1, response.items().size());            // merged, not duplicated
        assertEquals(5, response.items().get(0).quantity());
        assertEquals(0, new BigDecimal("50.00").compareTo(response.totalAmount()));
    }

    @Test
    void quantityAboveAvailableStockIsRejectedAndNothingIsSaved() {
        Inventory inventory = Inventory.builder().product(product).quantity(4).build();
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class, () -> cartService.addItem(5L, new CartItemRequest(1L, 3)));
        verify(cartRepository, never()).saveAndFlush(any());
    }
}
