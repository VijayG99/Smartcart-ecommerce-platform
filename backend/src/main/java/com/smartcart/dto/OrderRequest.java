package com.smartcart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderRequest(
        @Schema(example = "12 MG Road, Bengaluru 560001") @NotBlank @Size(max = 500) String shippingAddress) {
}
