package com.careerpilot.dto;

public record SkillEvidenceDto(
        String name,
        String category,
        boolean hasContext,
        String evidence
) {}
