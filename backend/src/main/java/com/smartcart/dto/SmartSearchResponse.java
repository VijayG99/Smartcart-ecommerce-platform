package com.smartcart.dto;

import java.math.BigDecimal;
import java.util.List;

/** Shows how the free-text query was interpreted, so the behaviour stays explainable. */
public record SmartSearchResponse(String originalQuery, List<String> keywords, BigDecimal minPrice,
                                  BigDecimal maxPrice, boolean cheapFirst, List<ProductResponse> results) {
}
