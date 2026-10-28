package com.careerpilot.entity;

public enum ApplicationStatus {
    WISHLIST,
    APPLIED,
    SCREENING,
    INTERVIEW,
    ASSESSMENT,
    OFFER,
    REJECTED,
    WITHDRAWN;

    public static ApplicationStatus fromString(String value) {
        if (value == null) return WISHLIST;
        String normalized = value.trim().toUpperCase();
        if ("INTERVIEWING".equals(normalized)) {
            return INTERVIEW;
        }
        try {
            return ApplicationStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return WISHLIST;
        }
    }
}
