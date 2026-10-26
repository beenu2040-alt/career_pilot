package com.careerpilot.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    String fullName,

    @Size(max = 255, message = "Headline must not exceed 255 characters")
    String headline,

    @Size(max = 255, message = "Location must not exceed 255 characters")
    String location,

    @Size(max = 255, message = "Target role must not exceed 255 characters")
    String targetRole
) {
}
