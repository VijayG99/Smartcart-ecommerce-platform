package com.smartcart.service;

import com.smartcart.dto.OrderRequest;
import com.smartcart.dto.OrderResponse;
import com.smartcart.entity.*;
import com.smartcart.event.OrderCreatedEvent;
import com.smartcart.exception.BadRequestException;
import com.smartcart.exception.InsufficientStockException;
import com.smartcart.exception.InvalidOrderStateException;
import com.smartcart.repository.CartRepository;
import com.smartcart.repository.OrderRepository;
import com.smartcart.repository.UserRepository;
import com.smartcart.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock UserRepository userRepository;
    @Mock InventoryService inventoryService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks OrderServiceImpl orderService;

    private final User user = User.builder().id(5L).email("c@x.com").build();

    private Product product(long id, String price) {
        return Product.builder().id(id).name("P" + id).price(new BigDecimal(price)).build();
    }

    private Cart cartWith(Product p, int qty) {
        Cart cart = Cart.builder().id(1L).user(user).build();
        cart.getItems().add(CartItem.builder().id(9L).cart(cart).product(p).quantity(qty).build());
        return cart;
    }

    @Test
    void placingOrderReducesStockClearsCartAndPublishesEvent() {
        Product p = product(1, "100.00");
        Cart cart = cartWith(p, 2);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });

        OrderResponse response = orderService.placeOrder(5L, new OrderRequest("Somewhere 1"));

        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(0, new BigDecimal("200.00").compareTo(response.totalAmount()));
        verify(inventoryService).reduceStock(1L, 2);
        assertTrue(cart.getItems().isEmpty());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(99L, ((OrderCreatedEvent) captor.getValue()).orderId());
    }

    @Test
    void insufficientStockAbortsTheOrderWithoutSavingOrPublishing() {
        Cart cart = cartWith(product(1, "100.00"), 5);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(cart));
        doThrow(new InsufficientStockException("no stock")).when(inventoryService).reduceStock(1L, 5);

        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(5L, new OrderRequest("Addr")));

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        assertEquals(1, cart.getItems().size());   // cart untouched
    }

    @Test
    void emptyCartCannotBeOrdered() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(5L)).thenReturn(Optional.of(Cart.builder().user(user).build()));
        assertThrows(BadRequestException.class, () -> orderService.placeOrder(5L, new OrderRequest("Addr")));
    }

    @Test
    void deliveredOrderCannotChangeStatus() {
        Order order = Order.builder().id(1L).user(user).status(OrderStatus.DELIVERED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertThrows(InvalidOrderStateException.class, () -> orderService.updateStatus(1L, OrderStatus.CONFIRMED));
    }

    @Test
    void cancellingReturnsStockToInventory() {
        Order order = Order.builder().id(1L).user(user).status(OrderStatus.PENDING).totalAmount(BigDecimal.TEN).build();
        order.getItems().add(OrderItem.builder().order(order).product(product(1, "5.00")).productName("P1")
                .unitPrice(new BigDecimal("5.00")).quantity(2).build());
        order.getItems().add(OrderItem.builder().order(order).product(product(2, "5.00")).productName("P2")
                .unitPrice(new BigDecimal("5.00")).quantity(3).build());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.updateStatus(1L, OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, response.status());
        verify(inventoryService).restoreStock(1L, 2);
        verify(inventoryService).restoreStock(2L, 3);
    }

    @Test
    void confirmOrderIsIdempotent() {
        Order order = Order.builder().id(1L).user(user).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        orderService.confirmOrder(1L);
        orderService.confirmOrder(1L);   // duplicate Kafka delivery
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }
}
