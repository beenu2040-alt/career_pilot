package com.careerpilot.controller;

import com.careerpilot.dto.*;
import com.careerpilot.security.SecurityUtils;
import com.careerpilot.service.CareerRoadmapService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/roadmaps")
public class CareerRoadmapController {

    private final CareerRoadmapService careerRoadmapService;

    public CareerRoadmapController(CareerRoadmapService careerRoadmapService) {
        this.careerRoadmapService = careerRoadmapService;
    }

    @PostMapping("/generate")
    public ResponseEntity<RoadmapDetailDto> generateRoadmap(
            @RequestBody(required = false) GenerateRoadmapRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        RoadmapDetailDto roadmap = careerRoadmapService.generateRoadmap(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(roadmap);
    }

    @GetMapping
    public ResponseEntity<List<RoadmapSummaryDto>> getRoadmaps(Authentication authentication) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        List<RoadmapSummaryDto> roadmaps = careerRoadmapService.getRoadmapsForUser(userId);
        return ResponseEntity.ok(roadmaps);
    }

    @GetMapping("/latest")
    public ResponseEntity<RoadmapDetailDto> getLatestRoadmap(Authentication authentication) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        Optional<RoadmapDetailDto> latest = careerRoadmapService.getLatestRoadmap(userId);
        return latest.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoadmapDetailDto> getRoadmapById(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        RoadmapDetailDto roadmap = careerRoadmapService.getRoadmapDetail(id, userId);
        return ResponseEntity.ok(roadmap);
    }

    @PatchMapping("/{id}/milestones/{milestoneId}")
    public ResponseEntity<RoadmapDetailDto> updateMilestoneStatus(
            @PathVariable("id") UUID id,
            @PathVariable("milestoneId") UUID milestoneId,
            @Valid @RequestBody UpdateMilestoneRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        RoadmapDetailDto updated = careerRoadmapService.updateMilestoneStatus(id, milestoneId, userId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoadmap(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        careerRoadmapService.deleteRoadmap(id, userId);
        return ResponseEntity.noContent().build();
    }
}
