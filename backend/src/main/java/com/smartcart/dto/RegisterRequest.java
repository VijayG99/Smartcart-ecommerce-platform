package com.smartcart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "Asha Rao") @NotBlank @Size(max = 100) String name,
        @Schema(example = "asha@example.com") @NotBlank @Email @Size(max = 150) String email,
        @Schema(example = "Str0ngPass!") @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password) {
}
