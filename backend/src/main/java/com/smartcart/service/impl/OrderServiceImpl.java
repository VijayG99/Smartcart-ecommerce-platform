package com.smartcart.service.impl;

import com.smartcart.dto.OrderRequest;
import com.smartcart.dto.OrderResponse;
import com.smartcart.entity.*;
import com.smartcart.event.OrderCreatedEvent;
import com.smartcart.exception.BadRequestException;
import com.smartcart.exception.InvalidOrderStateException;
import com.smartcart.exception.ResourceNotFoundException;
import com.smartcart.mapper.OrderMapper;
import com.smartcart.repository.CartRepository;
import com.smartcart.repository.OrderRepository;
import com.smartcart.repository.UserRepository;
import com.smartcart.service.InventoryService;
import com.smartcart.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * ONE transaction covers: validate cart -> reduce stock -> create order + items -> clear cart.
     * If any step throws (for example InsufficientStockException) everything is rolled back:
     * no partial order, no lost stock, and the cart is left untouched.
     * The Kafka event is published only after the commit succeeds (see OrderEventPublisher).
     */
    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Cart cart = cartRepository.findByUserId(userId)
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        Order order = Order.builder()
                .user(user).status(OrderStatus.PENDING)
                .shippingAddress(request.shippingAddress().trim())
                .totalAmount(BigDecimal.ZERO)
                .build();

        // Always lock stock rows in the same order (by product id) so concurrent orders cannot deadlock.
        List<CartItem> lines = cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId())).toList();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem line : lines) {
            Product product = line.getProduct();
            if (!product.isActive()) {
                throw new BadRequestException("Product is no longer available: " + product.getName());
            }
            inventoryService.reduceStock(product.getId(), line.getQuantity());
            order.getItems().add(OrderItem.builder().order(order).product(product).productName(product.getName())
                    .unitPrice(product.getPrice()).quantity(line.getQuantity()).build());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }
        order.setTotalAmount(total);
        orderRepository.save(order);

        cart.getItems().clear();   // orphanRemoval deletes the cart lines in the same transaction

        eventPublisher.publishEvent(toEvent(order, user));
        log.info("Order {} created for user {} total={}", order.getId(), userId, total);
        return OrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId, Long callerId, boolean callerIsAdmin) {
        Order order = find(orderId);
        if (!callerIsAdmin && !order.getUser().getId().equals(callerId)) {
            throw new AccessDeniedException("Not your order");
        }
        return OrderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getByUser(Long userId, Long callerId, boolean callerIsAdmin, Pageable pageable) {
        if (!callerIsAdmin && !userId.equals(callerId)) {
            throw new AccessDeniedException("Not your orders");
        }
        return orderRepository.findByUserId(userId, pageable).map(OrderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderMapper::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = find(orderId);
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidOrderStateException("Cannot change order from " + order.getStatus() + " to " + newStatus);
        }
        if (newStatus == OrderStatus.CANCELLED) {
            order.getItems().stream().sorted(Comparator.comparing(i -> i.getProduct().getId()))
                    .forEach(i -> inventoryService.restoreStock(i.getProduct().getId(), i.getQuantity()));
        }
        order.setStatus(newStatus);
        log.info("Order {} status -> {}", orderId, newStatus);
        return OrderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = find(orderId);
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            log.info("Order {} confirmed asynchronously", orderId);
        }   // any other status: duplicate delivery, nothing to do (idempotent)
    }

    private Order find(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private OrderCreatedEvent toEvent(Order o, User user) {
        List<OrderCreatedEvent.Item> items = o.getItems().stream()
                .map(i -> new OrderCreatedEvent.Item(i.getProduct().getId(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderCreatedEvent(o.getId(), user.getId(), user.getEmail(), o.getTotalAmount(), items, Instant.now());
    }
}
