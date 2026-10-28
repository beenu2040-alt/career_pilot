package com.careerpilot.controller;

import com.careerpilot.dto.CareerAnalyticsDto;
import com.careerpilot.security.SecurityUtils;
import com.careerpilot.service.CareerAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class CareerAnalyticsController {

    private final CareerAnalyticsService analyticsService;

    public CareerAnalyticsController(CareerAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/applications")
    public ResponseEntity<CareerAnalyticsDto> getApplicationAnalytics(Authentication authentication) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        CareerAnalyticsDto analytics = analyticsService.getAnalytics(userId);
        return ResponseEntity.ok(analytics);
    }
}
