package com.careerpilot.dto;

import java.time.Instant;

public record HealthResponse(
    String status,
    String service,
    String timestamp,
    String version
) {
    public static HealthResponse up() {
        return new HealthResponse("UP", "CareerPilot AI API", Instant.now().toString(), "0.1.0");
    }
}
