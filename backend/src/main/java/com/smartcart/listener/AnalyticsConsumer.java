package com.smartcart.listener;

import com.smartcart.event.OrderCreatedEvent;
import com.smartcart.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsConsumer {
    private final AnalyticsService analyticsService;

    @KafkaListener(id = "analytics-listener", topics = "${smartcart.kafka.order-topic}", groupId = "analytics-group")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("[analytics] received order {}", event.orderId());
        analyticsService.record(event);
    }
}
