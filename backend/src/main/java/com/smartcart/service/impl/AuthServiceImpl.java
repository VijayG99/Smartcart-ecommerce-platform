package com.smartcart.service.impl;

import com.smartcart.dto.*;
import com.smartcart.entity.Role;
import com.smartcart.entity.User;
import com.smartcart.exception.DuplicateResourceException;
import com.smartcart.mapper.UserMapper;
import com.smartcart.repository.UserRepository;
import com.smartcart.security.CustomUserDetails;
import com.smartcart.security.JwtService;
import com.smartcart.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        // Public registration can only create CUSTOMER accounts. Admins are created by the seeder / database.
        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(user);
        log.info("Registered new customer id={}", user.getId());
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        User user = userRepository.findById(principal.getId()).orElseThrow();
        log.info("User id={} logged in", principal.getId());
        return new AuthResponse(jwtService.generateToken(principal), "Bearer", jwtService.getExpirationMs(),
                UserMapper.toResponse(user));
    }
}
