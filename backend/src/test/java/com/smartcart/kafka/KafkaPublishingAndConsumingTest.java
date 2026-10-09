package com.smartcart.kafka;

import com.smartcart.event.OrderCreatedEvent;
import com.smartcart.listener.AnalyticsConsumer;
import com.smartcart.listener.InventoryConsumer;
import com.smartcart.listener.NotificationConsumer;
import com.smartcart.repository.AnalyticsRepository;
import com.smartcart.service.AnalyticsService;
import com.smartcart.service.InventoryService;
import com.smartcart.service.NotificationService;
import com.smartcart.service.OrderService;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Unit tests of the producer and the three consumers (no broker needed). */
@ExtendWith(MockitoExtension.class)
class KafkaPublishingAndConsumingTest {
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @Mock OrderService orderService;
    @Mock InventoryService inventoryService;
    @Mock NotificationService notificationService;
    @Mock AnalyticsRepository analyticsRepository;

    private final OrderCreatedEvent event = new OrderCreatedEvent(1L, 5L, "c@x.com", new BigDecimal("200.00"),
            List.of(new OrderCreatedEvent.Item(10L, "Lamp", 2, new BigDecimal("100.00"))), Instant.now());

    @SuppressWarnings("unchecked")
    @Test
    void publisherSendsEventToOrderTopicKeyedByOrderId() {
        OrderEventPublisher publisher = new OrderEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "topic", "order-events");
        SendResult<String, Object> result = mock(SendResult.class, RETURNS_DEEP_STUBS);
        when(kafkaTemplate.send(eq("order-events"), eq("1"), any())).thenReturn(CompletableFuture.completedFuture(result));

        publisher.publish(event);

        verify(kafkaTemplate).send("order-events", "1", event);
    }

    @Test
    void publisherDoesNotFailTheRequestWhenKafkaIsDown() {
        OrderEventPublisher publisher = new OrderEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "topic", "order-events");
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new KafkaException("broker down"));

        assertDoesNotThrow(() -> publisher.publish(event));
    }

    @Test
    void inventoryConsumerConfirmsOrderAndChecksStock() {
        new InventoryConsumer(orderService, inventoryService).onOrderCreated(event);

        verify(orderService).confirmOrder(1L);
        verify(inventoryService).warnIfLowStock(10L);
    }

    @Test
    void notificationConsumerSendsNotification() {
        new NotificationConsumer(notificationService).onOrderCreated(event);
        verify(notificationService).sendOrderPlaced(event);
    }

    @Test
    void analyticsConsumerStoresEventOnlyOnce() {
        AnalyticsConsumer consumer = new AnalyticsConsumer(new AnalyticsService(analyticsRepository));

        when(analyticsRepository.existsByOrderId(1L)).thenReturn(false, true);
        consumer.onOrderCreated(event);
        consumer.onOrderCreated(event);   // duplicate delivery

        verify(analyticsRepository, times(1)).save(any());
    }
}
