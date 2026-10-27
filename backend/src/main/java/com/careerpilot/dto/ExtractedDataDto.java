package com.careerpilot.dto;

import java.util.List;

public record ExtractedDataDto(
        String candidateName,
        String email,
        String phone,
        String location,
        List<String> detectedSections,
        int wordCount,
        int quantifiableMetricsCount,
        List<String> education,
        List<String> roles
) {}
