package com.smartcart.controller;

import com.smartcart.config.OpenApiConfig;
import com.smartcart.dto.AnalyticsSummary;
import com.smartcart.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics")
@SecurityRequirement(name = OpenApiConfig.BEARER)
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @Operation(summary = "Order count and revenue, built from Kafka events by the analytics consumer (ADMIN)")
    @GetMapping("/summary")
    public AnalyticsSummary summary() {
        return analyticsService.summary();
    }
}
