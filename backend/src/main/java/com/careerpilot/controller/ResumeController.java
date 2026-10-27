package com.careerpilot.controller;

import com.careerpilot.dto.*;
import com.careerpilot.security.SecurityUtils;
import com.careerpilot.service.ResumeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeSummaryDto> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        ResumeSummaryDto result = resumeService.uploadResume(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public ResponseEntity<List<ResumeSummaryDto>> getUserResumes(Authentication authentication) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        List<ResumeSummaryDto> list = resumeService.getUserResumes(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeSummaryDto> getResumeById(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        ResumeSummaryDto result = resumeService.getResumeById(id, userId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        resumeService.deleteResume(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<ResumeAnalysisDto> analyzeResume(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) AnalyzeResumeRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        Optional<UUID> targetJobId = request != null && request.jobId() != null
                ? Optional.of(request.jobId())
                : Optional.empty();

        ResumeAnalysisDto result = resumeService.analyzeResume(id, userId, targetJobId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/analyses")
    public ResponseEntity<List<ResumeAnalysisDto>> getResumeAnalyses(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        List<ResumeAnalysisDto> list = resumeService.getResumeAnalyses(id, userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}/analyses/{analysisId}")
    public ResponseEntity<ResumeAnalysisDto> getResumeAnalysisById(
            @PathVariable("id") UUID id,
            @PathVariable("analysisId") UUID analysisId,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        ResumeAnalysisDto result = resumeService.getResumeAnalysisById(analysisId, userId);
        return ResponseEntity.ok(result);
    }
}
