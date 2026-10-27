package com.careerpilot.controller;

import com.careerpilot.dto.*;
import com.careerpilot.security.SecurityUtils;
import com.careerpilot.service.JobService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * Browse and search active job listings with multi-dimensional filtering.
     * Open to all users; authenticated users receive personalized match scores and saved status.
     */
    @GetMapping
    public ResponseEntity<PageResponse<JobSummaryResponse>> getJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String remoteType,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "postedAt,desc") String sort,
            Authentication authentication
    ) {
        // Enforce valid pagination parameters
        int pageNumber = Math.max(0, page);
        int pageSize = Math.max(1, Math.min(50, size));

        Sort sortObj = parseSortParameter(sort);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortObj);

        UUID currentUserId = SecurityUtils.getUserIdOrNull(authentication);

        PageResponse<JobSummaryResponse> response = jobService.getJobs(
                search,
                location,
                employmentType,
                remoteType,
                experienceLevel,
                skill,
                currentUserId,
                pageable
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve job detail by ID. If authenticated, computes detailed explainable match.
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobDetailResponse> getJobDetail(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID currentUserId = SecurityUtils.getUserIdOrNull(authentication);
        JobDetailResponse response = jobService.getJobDetail(id, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve all jobs saved by the authenticated user.
     */
    @GetMapping("/saved")
    public ResponseEntity<PageResponse<JobSummaryResponse>> getSavedJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        int pageNumber = Math.max(0, page);
        int pageSize = Math.max(1, Math.min(50, size));
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());

        PageResponse<JobSummaryResponse> response = jobService.getSavedJobs(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Save a job to the authenticated user's bookmarks.
     */
    @PostMapping("/{id}/save")
    public ResponseEntity<SaveJobResponse> saveJob(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        SaveJobResponse response = jobService.saveJob(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a job from the authenticated user's bookmarks.
     */
    @DeleteMapping("/{id}/save")
    public ResponseEntity<SaveJobResponse> unsaveJob(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        SaveJobResponse response = jobService.unsaveJob(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve personalized job recommendations ranked by deterministic match percentage.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<PageResponse<JobSummaryResponse>> getRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        int pageNumber = Math.max(0, page);
        int pageSize = Math.max(1, Math.min(50, size));
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        PageResponse<JobSummaryResponse> response = jobService.getRecommendations(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Calculate explainable skill match breakdown for a single job for the authenticated user.
     */
    @GetMapping("/{id}/match")
    public ResponseEntity<JobMatchResponse> getJobMatch(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        JobMatchResponse response = jobService.getJobMatch(id, userId);
        return ResponseEntity.ok(response);
    }

    private Sort parseSortParameter(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by("postedAt").descending();
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // Whitelist allowable sort fields to prevent unexpected property lookups
        return switch (field) {
            case "title" -> Sort.by(direction, "title");
            case "company" -> Sort.by(direction, "company");
            case "location" -> Sort.by(direction, "location");
            case "createdAt" -> Sort.by(direction, "createdAt");
            default -> Sort.by(direction, "postedAt");
        };
    }
}
