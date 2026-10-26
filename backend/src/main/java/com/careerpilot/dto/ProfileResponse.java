package com.careerpilot.dto;

import com.careerpilot.entity.Profile;
import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
    UUID userId,
    String fullName,
    String headline,
    String location,
    String targetRole,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProfileResponse fromEntity(Profile profile) {
        return new ProfileResponse(
            profile.getUserId(),
            profile.getFullName(),
            profile.getHeadline(),
            profile.getLocation(),
            profile.getTargetRole(),
            profile.getCreatedAt(),
            profile.getUpdatedAt()
        );
    }
}
