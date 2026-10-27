package com.careerpilot.service;

import com.careerpilot.entity.Job;
import java.util.Optional;

public interface ResumeAnalysisClient {

    /**
     * Performs structured resume analysis against the rubric and optional target job.
     */
    ResumeAnalysisResult analyze(String resumeText, Optional<Job> targetJob);

    /**
     * Provider identifier (e.g., "gemini-1.5-flash", "deterministic-v1").
     */
    String getModelIdentifier();
}
