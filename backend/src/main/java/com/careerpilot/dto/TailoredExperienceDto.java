package com.careerpilot.dto;

import java.util.List;

public record TailoredExperienceDto(
        String roleTitle,
        String company,
        String duration,
        List<String> bullets
) {}
