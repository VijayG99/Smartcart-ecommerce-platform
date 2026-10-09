package com.smartcart.controller;

import com.smartcart.config.OpenApiConfig;
import com.smartcart.dto.*;
import com.smartcart.service.ProductService;
import com.smartcart.service.SmartSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Set;

@Tag(name = "Products")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private static final Set<String> SORTABLE = Set.of("name", "price", "createdAt");

    private final ProductService productService;
    private final SmartSearchService smartSearchService;

    @Operation(summary = "Search, filter, sort and paginate products (public)")
    @GetMapping
    public PageResponse<ProductResponse> search(
            @Parameter(description = "Text in name or description") @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "name | price | createdAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "asc | desc") @RequestParam(defaultValue = "desc") String direction) {
        String sortField = SORTABLE.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = Sort.by("asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), sort);
        return PageResponse.from(productService.search(
                new ProductSearchCriteria(q, categoryId, minPrice, maxPrice, inStock), pageable));
    }

    @Operation(summary = "Optional extension: natural-language search, e.g. 'affordable wireless headphones for office use'")
    @GetMapping("/smart-search")
    public SmartSearchResponse smartSearch(@RequestParam String query) {
        return smartSearchService.search(query);
    }

    @Operation(summary = "Get one product (served from Redis when cached)")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.getById(id);
    }

    @Operation(summary = "Create a product (ADMIN)")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "403", description = "Not an admin")
    @ApiResponse(responseCode = "409", description = "SKU already exists")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @Operation(summary = "Update a product (ADMIN)")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @Operation(summary = "Deactivate a product (ADMIN) - soft delete")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
