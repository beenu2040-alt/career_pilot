package com.careerpilot.dto;

import java.util.UUID;

public record SkillDto(
        UUID id,
        String name,
        String category,
        boolean isRequired
) {}
