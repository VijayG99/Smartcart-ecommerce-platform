package com.smartcart.listener;

import com.smartcart.event.OrderCreatedEvent;
import com.smartcart.service.InventoryService;
import com.smartcart.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Stock is already reduced synchronously (so we can never oversell). This consumer does the follow-up work:
 * it confirms the order (PENDING -> CONFIRMED) and raises low-stock warnings. Because it is asynchronous,
 * a customer may briefly see PENDING: that is eventual consistency.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryConsumer {
    private final OrderService orderService;
    private final InventoryService inventoryService;

    @KafkaListener(id = "inventory-listener", topics = "${smartcart.kafka.order-topic}", groupId = "inventory-group")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("[inventory] received order {}", event.orderId());
        orderService.confirmOrder(event.orderId());
        event.items().forEach(item -> inventoryService.warnIfLowStock(item.productId()));
    }
}
