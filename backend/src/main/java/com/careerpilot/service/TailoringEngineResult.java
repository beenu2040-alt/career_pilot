package com.careerpilot.service;

import com.careerpilot.dto.KeywordCoverageDto;
import com.careerpilot.dto.TailoredResumeContentDto;
import com.careerpilot.dto.TailoringSuggestionDto;

import java.util.List;

public record TailoringEngineResult(
        String targetRole,
        String modelIdentifier,
        int matchScoreBefore,
        int matchScoreAfter,
        KeywordCoverageDto keywordCoverage,
        List<TailoringSuggestionDto> suggestions,
        TailoredResumeContentDto baselineTailoredContent
) {}
