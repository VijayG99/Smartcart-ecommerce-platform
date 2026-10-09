package com.smartcart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuantityRequest(@Schema(example = "3") @NotNull @Positive @Max(1000) Integer quantity) {
}
