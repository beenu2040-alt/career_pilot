package com.careerpilot.controller;

import com.careerpilot.dto.*;
import com.careerpilot.security.SecurityUtils;
import com.careerpilot.service.ResumeTailoringService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resume-tailoring")
public class ResumeTailoringController {

    private final ResumeTailoringService tailoringService;

    public ResumeTailoringController(ResumeTailoringService tailoringService) {
        this.tailoringService = tailoringService;
    }

    @PostMapping
    public ResponseEntity<TailoringSessionDto> createSession(
            @Valid @RequestBody CreateTailoringSessionRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        TailoringSessionDto created = tailoringService.createSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<TailoringSessionDto>> getUserSessions(Authentication authentication) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        List<TailoringSessionDto> sessions = tailoringService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TailoringSessionDetailDto> getSessionDetails(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        TailoringSessionDetailDto details = tailoringService.getSessionDetails(id, userId);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<TailoringSessionDetailDto> generateSuggestions(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        TailoringSessionDetailDto result = tailoringService.generateSuggestions(id, userId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/suggestions/{suggestionId}")
    public ResponseEntity<TailoringSuggestionDto> updateSuggestion(
            @PathVariable("id") UUID id,
            @PathVariable("suggestionId") UUID suggestionId,
            @Valid @RequestBody UpdateSuggestionRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        TailoringSuggestionDto updated = tailoringService.updateSuggestion(id, suggestionId, userId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/versions")
    public ResponseEntity<TailoredResumeVersionDto> saveVersion(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) SaveTailoredVersionRequest request,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        SaveTailoredVersionRequest req = (request != null) ? request : new SaveTailoredVersionRequest(null, null, null);
        TailoredResumeVersionDto saved = tailoringService.saveVersion(id, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}/versions/{versionId}")
    public ResponseEntity<TailoredResumeVersionDto> getVersion(
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        TailoredResumeVersionDto version = tailoringService.getVersion(id, versionId, userId);
        return ResponseEntity.ok(version);
    }

    @GetMapping("/{id}/versions/{versionId}/export")
    public ResponseEntity<byte[]> exportVersionPdf(
            @PathVariable("id") UUID id,
            @PathVariable("versionId") UUID versionId,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        byte[] pdfBytes = tailoringService.exportVersionPdf(id, versionId, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = "tailored-resume-" + id.toString().substring(0, 8) + "-v" + versionId.toString().substring(0, 4) + ".pdf";
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(authentication);
        tailoringService.deleteSession(id, userId);
        return ResponseEntity.noContent().build();
    }
}
