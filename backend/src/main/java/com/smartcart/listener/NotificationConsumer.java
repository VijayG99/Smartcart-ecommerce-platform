package com.smartcart.listener;

import com.smartcart.event.OrderCreatedEvent;
import com.smartcart.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationService notificationService;

    @KafkaListener(id = "notification-listener", topics = "${smartcart.kafka.order-topic}", groupId = "notification-group")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("[notification] received order {}", event.orderId());
        notificationService.sendOrderPlaced(event);
    }
}
