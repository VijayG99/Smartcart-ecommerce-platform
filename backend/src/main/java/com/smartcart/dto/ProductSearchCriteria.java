package com.smartcart.dto;

import java.math.BigDecimal;

public record ProductSearchCriteria(String q, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStock) {
}
