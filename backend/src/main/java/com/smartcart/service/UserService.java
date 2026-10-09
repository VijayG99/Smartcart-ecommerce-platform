package com.smartcart.service;

import com.smartcart.dto.UserResponse;

public interface UserService {
    UserResponse getProfile(Long userId);
}
