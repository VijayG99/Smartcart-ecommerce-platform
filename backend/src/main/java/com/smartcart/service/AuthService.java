package com.smartcart.service;

import com.smartcart.dto.AuthResponse;
import com.smartcart.dto.LoginRequest;
import com.smartcart.dto.RegisterRequest;
import com.smartcart.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
