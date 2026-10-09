package com.smartcart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @Schema(example = "Wireless Headphones") @NotBlank @Size(max = 200) String name,
        @Schema(example = "Bluetooth over-ear headphones with noise cancelling") @Size(max = 2000) String description,
        @Schema(example = "2499.00") @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Schema(example = "HP-WL-001") @NotBlank @Size(max = 64) String sku,
        @Schema(example = "1") @NotNull Long categoryId,
        @Schema(description = "Used only on create. Use the inventory API to change stock later.", example = "25")
        @Min(0) @Max(1_000_000) Integer initialQuantity) {
}
