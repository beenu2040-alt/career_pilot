package com.careerpilot.dto;

import java.util.List;

public record TailoredResumeContentDto(
        String candidateName,
        String email,
        String phone,
        String location,
        String professionalSummary,
        List<String> skills,
        List<TailoredExperienceDto> experience,
        List<TailoredProjectDto> projects,
        List<String> education,
        List<String> formattingNotes
) {}
