package com.smartcart.service.impl;

import com.smartcart.dto.UserResponse;
import com.smartcart.exception.ResourceNotFoundException;
import com.smartcart.mapper.UserMapper;
import com.smartcart.repository.UserRepository;
import com.smartcart.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId).map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
