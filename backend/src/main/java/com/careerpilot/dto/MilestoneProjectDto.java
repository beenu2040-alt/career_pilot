package com.careerpilot.dto;

import java.util.List;

public record MilestoneProjectDto(
        String title,
        String description,
        String deliverable,
        List<String> techStack
) {}
