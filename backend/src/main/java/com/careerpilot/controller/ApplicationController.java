package com.careerpilot.controller;

import com.careerpilot.dto.*;
import com.careerpilot.entity.ApplicationStatus;
import com.careerpilot.security.SecurityUtils;
import com.careerpilot.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationDto> createApplication(
            @Valid @RequestBody CreateApplicationRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        ApplicationDto created = applicationService.createApplication(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ApplicationDto>> getApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        PageResponse<ApplicationDto> response = applicationService.getApplications(userId, status, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ApplicationDto>> getAllApplications(Authentication authentication) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        List<ApplicationDto> list = applicationService.getAllUserApplications(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/reminders")
    public ResponseEntity<RemindersSummaryDto> getReminders(Authentication authentication) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        RemindersSummaryDto reminders = applicationService.getReminders(userId);
        return ResponseEntity.ok(reminders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDto> getApplicationById(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        ApplicationDto app = applicationService.getApplicationById(id, userId);
        return ResponseEntity.ok(app);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationDto> updateApplication(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateApplicationRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        ApplicationDto updated = applicationService.updateApplication(id, userId, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationDto> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        ApplicationDto updated = applicationService.updateStatus(id, userId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        applicationService.deleteApplication(id, userId);
        return ResponseEntity.noContent().build();
    }
}
