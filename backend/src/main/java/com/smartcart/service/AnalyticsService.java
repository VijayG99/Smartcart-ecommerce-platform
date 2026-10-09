package com.smartcart.service;

import com.smartcart.dto.AnalyticsSummary;
import com.smartcart.entity.AnalyticsRecord;
import com.smartcart.event.OrderCreatedEvent;
import com.smartcart.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final AnalyticsRepository analyticsRepository;

    /** Idempotent: Kafka guarantees at-least-once delivery, so the same event may arrive twice. */
    @Transactional
    public void record(OrderCreatedEvent event) {
        if (analyticsRepository.existsByOrderId(event.orderId())) {
            log.info("Analytics already recorded for order {}, skipping duplicate", event.orderId());
            return;
        }
        int itemCount = event.items().stream().mapToInt(OrderCreatedEvent.Item::quantity).sum();
        analyticsRepository.save(AnalyticsRecord.builder().orderId(event.orderId()).userId(event.userId())
                .totalAmount(event.totalAmount()).itemCount(itemCount).recordedAt(Instant.now()).build());
    }

    @Transactional(readOnly = true)
    public AnalyticsSummary summary() {
        return new AnalyticsSummary(analyticsRepository.count(), analyticsRepository.totalRevenue());
    }
}
