package com.careerpilot.dto;

import java.util.List;

public record SkillGapSummaryDto(
        int totalTargetSkills,
        int acquiredSkillsCount,
        int gapSkillsCount,
        int readinessScore,
        List<SkillGapDto> acquiredSkills,
        List<SkillGapDto> gapSkills,
        List<String> keyStrengths,
        List<String> priorityGaps
) {}
