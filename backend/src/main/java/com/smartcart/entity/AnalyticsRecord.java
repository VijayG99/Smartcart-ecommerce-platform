package com.smartcart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Written by the analytics Kafka consumer. Unique orderId makes the consumer idempotent. */
@Entity
@Table(name = "order_analytics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyticsRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private int itemCount;

    @Column(nullable = false)
    private Instant recordedAt;
}
