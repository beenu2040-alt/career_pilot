package com.careerpilot.dto;

public record SaveTailoredVersionRequest(
        String versionName,
        String changeSummary,
        TailoredResumeContentDto customContent
) {}
