package com.smartcart.dto;

import java.math.BigDecimal;

public record AnalyticsSummary(long totalOrders, BigDecimal totalRevenue) {
}
