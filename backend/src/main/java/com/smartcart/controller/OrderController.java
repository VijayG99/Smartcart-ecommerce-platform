package com.smartcart.controller;

import com.smartcart.config.OpenApiConfig;
import com.smartcart.dto.*;
import com.smartcart.security.CustomUserDetails;
import com.smartcart.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders")
@SecurityRequirement(name = OpenApiConfig.BEARER)
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");
    private final OrderService orderService;

    @Operation(summary = "Place an order from my cart")
    @ApiResponse(responseCode = "201", description = "Order created (status PENDING until confirmed asynchronously)")
    @ApiResponse(responseCode = "400", description = "Cart is empty")
    @ApiResponse(responseCode = "409", description = "Insufficient stock - nothing is changed")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@AuthenticationPrincipal CustomUserDetails me, @Valid @RequestBody OrderRequest request) {
        return orderService.placeOrder(me.getId(), request);
    }

    @Operation(summary = "Get one order (owner or ADMIN)")
    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id) {
        return orderService.getById(id, me.getId(), me.isAdmin());
    }

    @Operation(summary = "List a user's orders (that user or ADMIN)")
    @GetMapping("/user/{userId}")
    public PageResponse<OrderResponse> byUser(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long userId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return PageResponse.from(orderService.getByUser(userId, me.getId(), me.isAdmin(), pageable(page, size)));
    }

    @Operation(summary = "List all orders (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<OrderResponse> all(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return PageResponse.from(orderService.getAll(pageable(page, size)));
    }

    @Operation(summary = "Change order status (ADMIN). Cancelling returns stock to inventory.")
    @ApiResponse(responseCode = "409", description = "Transition not allowed")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), NEWEST_FIRST);
    }
}
