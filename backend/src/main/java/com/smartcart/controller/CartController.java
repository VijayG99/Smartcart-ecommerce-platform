package com.smartcart.controller;

import com.smartcart.config.OpenApiConfig;
import com.smartcart.dto.CartItemRequest;
import com.smartcart.dto.CartResponse;
import com.smartcart.dto.QuantityRequest;
import com.smartcart.security.CustomUserDetails;
import com.smartcart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cart")
@SecurityRequirement(name = OpenApiConfig.BEARER)
@PreAuthorize("hasRole('CUSTOMER')")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @Operation(summary = "View my cart")
    @GetMapping
    public CartResponse get(@AuthenticationPrincipal CustomUserDetails me) {
        return cartService.getCart(me.getId());
    }

    @Operation(summary = "Add a product to my cart (merges with an existing line)")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "Not enough stock")
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse add(@AuthenticationPrincipal CustomUserDetails me, @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(me.getId(), request);
    }

    @Operation(summary = "Change the quantity of a cart line")
    @PutMapping("/items/{id}")
    public CartResponse update(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id,
                               @Valid @RequestBody QuantityRequest request) {
        return cartService.updateItem(me.getId(), id, request);
    }

    @Operation(summary = "Remove a cart line")
    @DeleteMapping("/items/{id}")
    public CartResponse remove(@AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id) {
        return cartService.removeItem(me.getId(), id);
    }
}
