package com.smartcart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(
        @Schema(example = "1") @NotNull Long productId,
        @Schema(example = "2") @NotNull @Positive @Max(1000) Integer quantity) {
}
