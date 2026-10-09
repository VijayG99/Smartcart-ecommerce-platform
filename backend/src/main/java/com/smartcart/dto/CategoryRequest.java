package com.smartcart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @Schema(example = "Electronics") @NotBlank @Size(max = 100) String name,
        @Schema(example = "Gadgets and accessories") @Size(max = 500) String description) {
}
