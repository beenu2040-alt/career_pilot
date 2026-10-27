package com.careerpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserSkillCreateRequest(
        @NotBlank(message = "Skill name is required")
        String skillName,

        String category,

        @NotBlank(message = "Proficiency level is required")
        @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED|EXPERT", message = "Proficiency must be BEGINNER, INTERMEDIATE, ADVANCED, or EXPERT")
        String proficiencyLevel,

        Integer yearsExperience
) {}
