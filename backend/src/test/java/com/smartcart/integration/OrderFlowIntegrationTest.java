package com.smartcart.integration;

import com.smartcart.dto.*;
import com.smartcart.entity.OrderStatus;
import com.smartcart.event.OrderCreatedEvent;
import com.smartcart.exception.InsufficientStockException;
import com.smartcart.exception.InvalidOrderStateException;
import com.smartcart.repository.CategoryRepository;
import com.smartcart.repository.OrderRepository;
import com.smartcart.entity.Category;
import com.smartcart.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Real Spring context + real transactions (H2). Kafka is replaced by a mock template so we can assert that
 * the event is published after commit - and NOT published when the order transaction rolls back.
 * These tests are intentionally not @Transactional: the order transaction must really commit or roll back.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderFlowIntegrationTest {
    @SuppressWarnings("rawtypes")
    @MockBean KafkaTemplate kafkaTemplate;

    @Autowired AuthService authService;
    @Autowired ProductService productService;
    @Autowired InventoryService inventoryService;
    @Autowired CartService cartService;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired CategoryRepository categories;

    Long categoryId;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        categoryId = categories.save(Category.builder().name(TestData.name("cat")).build()).getId();
        SendResult<String, Object> result = mock(SendResult.class, RETURNS_DEEP_STUBS);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(result));
    }

    private Long newCustomer() {
        return authService.register(new RegisterRequest("Buyer", TestData.email(), "Password123")).id();
    }

    private Long newProduct(int stock) {
        return productService.create(new ProductRequest(TestData.name("Item"), "d", new BigDecimal("100.00"),
                TestData.sku(), categoryId, stock)).getId();
    }

    @Test
    @SuppressWarnings("unchecked")
    void successfulOrderReducesStockClearsCartAndPublishesEvent() {
        Long userId = newCustomer();
        Long productId = newProduct(5);
        cartService.addItem(userId, new CartItemRequest(productId, 2));

        OrderResponse order = orderService.placeOrder(userId, new OrderRequest("12 Main Street"));

        assertEquals(OrderStatus.PENDING, order.status());
        assertEquals(0, new BigDecimal("200.00").compareTo(order.totalAmount()));
        assertEquals(3, inventoryService.getAvailable(productId));
        assertTrue(cartService.getCart(userId).items().isEmpty());
        verify(kafkaTemplate).send(eq("order-events"), eq(String.valueOf(order.id())), any(OrderCreatedEvent.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void insufficientStockRollsEverythingBack() {
        Long userId = newCustomer();
        Long productId = newProduct(5);
        cartService.addItem(userId, new CartItemRequest(productId, 2));
        inventoryService.updateStock(productId, 1);   // someone else bought it meanwhile

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(userId, new OrderRequest("12 Main Street")));

        assertEquals(1, inventoryService.getAvailable(productId), "stock unchanged");
        assertEquals(1, cartService.getCart(userId).items().size(), "cart not cleared");
        assertEquals(0, orderRepository.findByUserId(userId, PageRequest.of(0, 5)).getTotalElements(), "no partial order");
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void cancellingAnOrderRestoresStockAndInvalidTransitionsAreRefused() {
        Long userId = newCustomer();
        Long productId = newProduct(5);
        cartService.addItem(userId, new CartItemRequest(productId, 2));
        OrderResponse order = orderService.placeOrder(userId, new OrderRequest("12 Main Street"));

        assertThrows(InvalidOrderStateException.class, () -> orderService.updateStatus(order.id(), OrderStatus.DELIVERED));

        orderService.updateStatus(order.id(), OrderStatus.CANCELLED);
        assertEquals(5, inventoryService.getAvailable(productId));
    }

    @Test
    void confirmingAsynchronouslyMovesPendingToConfirmed() {
        Long userId = newCustomer();
        Long productId = newProduct(5);
        cartService.addItem(userId, new CartItemRequest(productId, 1));
        OrderResponse order = orderService.placeOrder(userId, new OrderRequest("12 Main Street"));

        orderService.confirmOrder(order.id());

        assertEquals(OrderStatus.CONFIRMED, orderService.getById(order.id(), userId, false).status());
    }

    @Test
    void customerCannotReadSomeoneElsesOrder() {
        Long owner = newCustomer();
        Long stranger = newCustomer();
        Long productId = newProduct(5);
        cartService.addItem(owner, new CartItemRequest(productId, 1));
        OrderResponse order = orderService.placeOrder(owner, new OrderRequest("12 Main Street"));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> orderService.getById(order.id(), stranger, false));
        assertNotNull(orderService.getById(order.id(), stranger, true));   // admin may
    }

    @Test
    void twoCustomersRacingForTheLastUnitCanNeverOversell() throws Exception {
        Long productId = newProduct(1);
        Long a = newCustomer();
        Long b = newCustomer();
        cartService.addItem(a, new CartItemRequest(productId, 1));
        cartService.addItem(b, new CartItemRequest(productId, 1));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> results = List.of(
                pool.submit(() -> tryOrder(a, start)),
                pool.submit(() -> tryOrder(b, start)));
        start.countDown();
        long successes = 0;
        for (Future<Boolean> f : results) if (f.get(20, TimeUnit.SECONDS)) successes++;
        pool.shutdown();

        assertEquals(1, successes, "exactly one buyer gets the last unit");
        assertEquals(0, inventoryService.getAvailable(productId), "stock is zero, never negative");
    }

    private boolean tryOrder(Long userId, CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            orderService.placeOrder(userId, new OrderRequest("12 Main Street"));
            return true;
        } catch (InsufficientStockException e) {
            return false;
        }
    }
}
