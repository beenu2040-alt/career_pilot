package com.careerpilot.dto;

import com.careerpilot.entity.SuggestionSection;
import com.careerpilot.entity.SuggestionStatus;

import java.time.Instant;
import java.util.UUID;

public record TailoringSuggestionDto(
        UUID id,
        UUID sessionId,
        SuggestionSection section,
        String title,
        String originalContent,
        String suggestedContent,
        String userEditedContent,
        String explanation,
        SuggestionStatus status,
        boolean requiresUserConfirmation,
        String confirmationPrompt,
        int sequenceOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public TailoringSuggestionDto(
            UUID id,
            UUID sessionId,
            SuggestionSection section,
            String title,
            String originalContent,
            String suggestedContent,
            String userEditedContent,
            String explanation,
            SuggestionStatus status,
            boolean requiresUserConfirmation,
            String confirmationPrompt,
            int sequenceOrder
    ) {
        this(
                id,
                sessionId,
                section,
                title,
                originalContent,
                suggestedContent,
                userEditedContent,
                explanation,
                status,
                requiresUserConfirmation,
                confirmationPrompt,
                sequenceOrder,
                Instant.now(),
                Instant.now()
        );
    }
}
