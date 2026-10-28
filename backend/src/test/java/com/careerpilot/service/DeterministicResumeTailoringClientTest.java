package com.careerpilot.service;

import com.careerpilot.dto.TailoringSuggestionDto;
import com.careerpilot.entity.SuggestionSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicResumeTailoringClientTest {

    private DeterministicResumeTailoringClient tailoringClient;

    @BeforeEach
    void setUp() {
        SkillNormalizer normalizer = new SkillNormalizer();
        tailoringClient = new DeterministicResumeTailoringClient(normalizer);
    }

    @Test
    @DisplayName("tailor should generate structured suggestions preserving factual accuracy")
    void testTailor_StructuredOutputAndFactualAccuracy() {
        String resumeText = """
                Jane Doe
                jane.doe@example.com | (555) 123-4567 | San Francisco, CA
                
                PROFESSIONAL SUMMARY
                Software engineer with experience in Java, Spring Boot, and PostgreSQL.
                
                TECHNICAL SKILLS
                Languages & Frameworks: Java, Spring Boot, PostgreSQL, Docker, Git.
                
                EXPERIENCE
                Software Engineer — Acme Corp (2021 - Present)
                • Developed REST APIs for user management service.
                • Improved database query indexing.
                
                EDUCATION
                B.S. in Computer Science — UC Berkeley
                """;

        String jobDesc = """
                Senior Backend Engineer at FinTech Global.
                Requirements:
                - Strong expertise in Java and Spring Boot.
                - Experience with PostgreSQL and Docker.
                - Knowledge of Kubernetes and AWS cloud infrastructure.
                - Microservices design and REST API architecture.
                """;

        TailoringEngineResult result = tailoringClient.tailor(
                resumeText,
                "Senior Backend Engineer",
                "FinTech Global",
                jobDesc,
                Optional.empty()
        );

        assertNotNull(result);
        assertEquals("Senior Backend Engineer", result.targetRole());
        assertTrue(result.matchScoreBefore() > 0);
        assertTrue(result.matchScoreAfter() >= result.matchScoreBefore());

        // Verify keyword coverage
        assertNotNull(result.keywordCoverage());
        assertTrue(result.keywordCoverage().matchedKeywords().contains("Java"));
        assertTrue(result.keywordCoverage().matchedKeywords().contains("Spring Boot"));

        // Verify distinction between missing keywords and missing qualifications
        assertFalse(result.keywordCoverage().missingQualifications().isEmpty(),
                "Major qualifications like AWS / Kubernetes should be classified under missing qualifications");

        // Verify suggestions
        assertNotNull(result.suggestions());
        assertFalse(result.suggestions().isEmpty());

        // Check for confirmation flag on unsupported skill suggestions
        boolean hasConfirmationFlag = result.suggestions().stream()
                .anyMatch(TailoringSuggestionDto::requiresUserConfirmation);
        assertTrue(hasConfirmationFlag, "Suggestions suggesting skills not in source resume must require user confirmation");

        // Verify sections covered
        boolean hasSummary = result.suggestions().stream().anyMatch(s -> s.section() == SuggestionSection.SUMMARY);
        boolean hasSkills = result.suggestions().stream().anyMatch(s -> s.section() == SuggestionSection.SKILLS);
        boolean hasExperience = result.suggestions().stream().anyMatch(s -> s.section() == SuggestionSection.EXPERIENCE);

        assertTrue(hasSummary, "Should generate summary suggestion");
        assertTrue(hasSkills, "Should generate skills suggestion");
        assertTrue(hasExperience, "Should generate experience suggestion");
    }

    @Test
    @DisplayName("tailor should handle empty or sparse resume input gracefully")
    void testTailor_GracefulHandlingOfSparseInput() {
        TailoringEngineResult result = tailoringClient.tailor(
                "",
                "Junior Developer",
                "StartUp Co",
                "Seeking entry-level developer with Java knowledge",
                Optional.empty()
        );

        assertNotNull(result);
        assertNotNull(result.baselineTailoredContent());
        assertNotNull(result.suggestions());
        assertFalse(result.suggestions().isEmpty());
    }
}
