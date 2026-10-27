package com.careerpilot.dto;

import java.util.List;

public record SkillAnalysisDto(
        List<SkillEvidenceDto> technicalSkills,
        List<SkillEvidenceDto> softSkills,
        List<String> skillsWithoutEvidence
) {}
