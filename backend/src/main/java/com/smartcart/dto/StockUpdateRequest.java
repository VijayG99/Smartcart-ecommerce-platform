package com.smartcart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockUpdateRequest(@Schema(example = "40") @NotNull @Min(0) @Max(1_000_000) Integer quantity) {
}
