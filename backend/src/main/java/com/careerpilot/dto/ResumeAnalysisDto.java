package com.careerpilot.dto;

import com.careerpilot.entity.ResumeAnalysisStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResumeAnalysisDto(
        UUID id,
        UUID resumeId,
        UUID jobId,
        ResumeAnalysisStatus analysisStatus,
        Integer overallScore,
        String summary,
        List<DimensionScoreDto> dimensionScores,
        ExtractedDataDto extractedData,
        List<StrengthDto> strengths,
        List<ImprovementDto> improvements,
        SkillAnalysisDto skillAnalysis,
        ExperienceAnalysisDto experienceAnalysis,
        JobMatchAnalysisDto jobMatch,
        String modelIdentifier,
        Instant createdAt,
        Instant completedAt
) {}
