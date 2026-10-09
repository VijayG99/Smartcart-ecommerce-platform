package com.smartcart.controller;

import com.smartcart.config.OpenApiConfig;
import com.smartcart.dto.InventoryResponse;
import com.smartcart.dto.PageResponse;
import com.smartcart.dto.StockUpdateRequest;
import com.smartcart.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Inventory")
@SecurityRequirement(name = OpenApiConfig.BEARER)
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @Operation(summary = "List stock levels (ADMIN)")
    @GetMapping
    public PageResponse<InventoryResponse> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(inventoryService.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
    }

    @Operation(summary = "Get stock for one product (ADMIN)")
    @GetMapping("/{productId}")
    public InventoryResponse get(@PathVariable Long productId) {
        return inventoryService.getByProductId(productId);
    }

    @Operation(summary = "Set stock for one product (ADMIN)")
    @PutMapping("/{productId}")
    public InventoryResponse update(@PathVariable Long productId, @Valid @RequestBody StockUpdateRequest request) {
        return inventoryService.updateStock(productId, request.quantity());
    }
}
