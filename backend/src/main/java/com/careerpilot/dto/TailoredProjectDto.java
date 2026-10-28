package com.careerpilot.dto;

import java.util.List;

public record TailoredProjectDto(
        String title,
        String description,
        List<String> techStack,
        List<String> bullets
) {}
