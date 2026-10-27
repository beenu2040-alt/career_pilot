package com.careerpilot.service;

import com.careerpilot.dto.*;
import java.util.List;
import java.util.Map;

public record ResumeAnalysisResult(
        String summary,
        int overallScore,
        List<DimensionScoreDto> dimensionScores,
        ExtractedDataDto extractedData,
        List<StrengthDto> strengths,
        List<ImprovementDto> improvements,
        SkillAnalysisDto skillAnalysis,
        ExperienceAnalysisDto experienceAnalysis,
        JobMatchAnalysisDto jobMatch,
        String modelIdentifier
) {}
