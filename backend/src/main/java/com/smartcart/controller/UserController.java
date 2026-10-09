package com.smartcart.controller;

import com.smartcart.config.OpenApiConfig;
import com.smartcart.dto.UserResponse;
import com.smartcart.security.CustomUserDetails;
import com.smartcart.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@SecurityRequirement(name = OpenApiConfig.BEARER)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "Get the profile of the logged-in user")
    @GetMapping("/profile")
    public UserResponse profile(@AuthenticationPrincipal CustomUserDetails principal) {
        return userService.getProfile(principal.getId());
    }
}
