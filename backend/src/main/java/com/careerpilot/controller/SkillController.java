package com.careerpilot.controller;

import com.careerpilot.dto.SkillDto;
import com.careerpilot.dto.UserSkillCreateRequest;
import com.careerpilot.dto.UserSkillDto;
import com.careerpilot.security.SecurityUtils;
import com.careerpilot.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    /**
     * Get global catalog of skills available across the platform.
     */
    @GetMapping("/skills")
    public ResponseEntity<List<SkillDto>> getAllSkills() {
        List<SkillDto> skills = skillService.getAllSkills();
        return ResponseEntity.ok(skills);
    }

    /**
     * Get authenticated user's cataloged skills.
     */
    @GetMapping("/profile/skills")
    public ResponseEntity<List<UserSkillDto>> getUserSkills(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        List<UserSkillDto> skills = skillService.getUserSkills(userId);
        return ResponseEntity.ok(skills);
    }

    /**
     * Add or update a verified skill on user's profile.
     */
    @PostMapping("/profile/skills")
    public ResponseEntity<UserSkillDto> addUserSkill(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserSkillCreateRequest request
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        UserSkillDto userSkill = skillService.addUserSkill(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userSkill);
    }

    /**
     * Remove a skill from user's profile.
     */
    @DeleteMapping("/profile/skills/{skillId}")
    public ResponseEntity<Void> removeUserSkill(
            @PathVariable UUID skillId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = SecurityUtils.getAuthenticatedUserId(jwt);
        skillService.removeUserSkill(userId, skillId);
        return ResponseEntity.noContent().build();
    }
}
