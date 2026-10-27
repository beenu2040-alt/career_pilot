package com.careerpilot.dto;

import java.util.UUID;

public record UserSkillDto(
        UUID skillId,
        String skillName,
        String category,
        String proficiencyLevel,
        Integer yearsExperience
) {}
