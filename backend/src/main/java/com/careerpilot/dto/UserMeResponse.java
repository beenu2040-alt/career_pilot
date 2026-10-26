package com.careerpilot.dto;

import java.time.Instant;
import java.util.UUID;

public record UserMeResponse(
    UUID userId,
    String email,
    String role,
    Instant authenticatedAt
) {
}
