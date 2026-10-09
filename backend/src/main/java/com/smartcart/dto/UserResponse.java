package com.smartcart.dto;

import com.smartcart.entity.Role;

public record UserResponse(Long id, String name, String email, Role role) {
}
