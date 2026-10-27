package com.careerpilot.dto;

public record ImprovementDto(
        String priority, // HIGH, MEDIUM, LOW
        String category,
        String title,
        String issue,
        String suggestion,
        String exampleBefore,
        String exampleAfter
) {}
