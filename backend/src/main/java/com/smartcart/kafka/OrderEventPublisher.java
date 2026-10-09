package com.smartcart.kafka;

import com.smartcart.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * OrderService publishes a Spring event inside its transaction. This listener runs only AFTER COMMIT,
 * so Kafka never announces an order that was rolled back.
 *
 * Trade-off (explainable in interviews): if Kafka is down at that moment the order is saved but the event is lost.
 * The production fix is the Transactional Outbox pattern (see README, Future enhancements).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${smartcart.kafka.order-topic}")
    private String topic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderCreatedEvent event) {
        try {
            // Key = orderId: all events of one order go to the same partition, so they stay in order.
            kafkaTemplate.send(topic, String.valueOf(event.orderId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish OrderCreatedEvent for order {}", event.orderId(), ex);
                        } else {
                            log.info("Published OrderCreatedEvent order={} partition={} offset={}", event.orderId(),
                                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException ex) {
            // The order is already committed; do not fail the customer's request because messaging is down.
            log.error("Kafka unavailable, event for order {} was not published", event.orderId(), ex);
        }
    }
}
