package com.smartcart.mapper;

import com.smartcart.dto.UserResponse;
import com.smartcart.entity.User;

public final class UserMapper {
    private UserMapper() {}

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
