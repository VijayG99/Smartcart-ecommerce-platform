package com.smartcart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "asha@example.com") @NotBlank @Email String email,
        @Schema(example = "Str0ngPass!") @NotBlank String password) {
}
