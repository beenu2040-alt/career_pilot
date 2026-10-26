package com.careerpilot.controller;

import com.careerpilot.dto.ProfileResponse;
import com.careerpilot.dto.ProfileUpdateRequest;
import com.careerpilot.security.SecurityUtils;
import com.careerpilot.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        ProfileResponse response = profileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        ProfileResponse response = profileService.upsertProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
