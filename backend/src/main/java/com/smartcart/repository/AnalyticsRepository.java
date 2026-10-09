package com.smartcart.repository;

import com.smartcart.entity.AnalyticsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface AnalyticsRepository extends JpaRepository<AnalyticsRecord, Long> {
    boolean existsByOrderId(Long orderId);

    @Query("select coalesce(sum(a.totalAmount), 0) from AnalyticsRecord a")
    BigDecimal totalRevenue();
}
