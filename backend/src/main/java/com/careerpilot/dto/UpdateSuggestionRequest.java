package com.careerpilot.dto;

import com.careerpilot.entity.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSuggestionRequest(
        @NotNull(message = "Suggestion status is required")
        SuggestionStatus status,
        String userEditedContent
) {}
