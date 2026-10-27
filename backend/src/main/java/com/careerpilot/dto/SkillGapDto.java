package com.careerpilot.dto;

public record SkillGapDto(
        String skill,
        String category,
        String status,
        String priority,
        int marketDemandFrequency,
        String evidenceOrContext
) {}
