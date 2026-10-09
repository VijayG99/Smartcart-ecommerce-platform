package com.smartcart.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Deliberately a non-final class (not a record): it is stored in Redis as JSON with type information,
 * and Jackson's default typing only works for non-final types.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String sku;
    private Long categoryId;
    private String categoryName;
    private int availableQuantity;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
