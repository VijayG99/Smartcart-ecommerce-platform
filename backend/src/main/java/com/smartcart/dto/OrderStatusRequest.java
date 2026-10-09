package com.smartcart.dto;

import com.smartcart.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@Schema(example = "PROCESSING") @NotNull OrderStatus status) {
}
