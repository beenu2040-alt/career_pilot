package com.careerpilot.service;

import com.careerpilot.entity.Job;
import com.careerpilot.entity.JobSkill;
import com.careerpilot.entity.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicResumeAnalysisClientTest {

    private DeterministicResumeAnalysisClient client;

    @BeforeEach
    void setUp() {
        SkillNormalizer normalizer = new SkillNormalizer();
        client = new DeterministicResumeAnalysisClient(normalizer);
    }

    @Test
    void testAnalyze_ComprehensiveResume() {
        String resume = """
                ALEX MORGAN
                alex.morgan@example.com | (555) 123-4567 | San Francisco, CA
                
                PROFESSIONAL SUMMARY
                Senior Full-Stack Engineer with 6 years of experience building scalable cloud applications.
                
                TECHNICAL SKILLS
                Languages & Frameworks: Java, Spring Boot, React, TypeScript, Python
                Databases & Cloud: PostgreSQL, Redis, Docker, Kubernetes, AWS
                
                EXPERIENCE
                Senior Software Engineer - Acme Cloud Systems (2021 - Present)
                - Architected 8 distributed microservices in Java and Spring Boot serving 500,000 daily active users.
                - Optimized PostgreSQL indexing and queries, decreasing p99 API latency by 45%.
                - Spearheaded migration of frontend to React and TypeScript, increasing user engagement by 25%.
                - Containerized application services using Docker and deployed on AWS EKS clusters.
                
                Software Engineer - TechFlow Inc (2018 - 2021)
                - Developed responsive UI dashboards and REST APIs in Python and React.
                - Automated CI/CD deployment pipelines, saving 15 engineering hours weekly.
                
                EDUCATION
                Bachelor of Science in Computer Science - University of California, Berkeley
                """;

        ResumeAnalysisResult result = client.analyze(resume, Optional.empty());

        assertNotNull(result);
        assertEquals("careerpilot-deterministic-rubric-v1", result.modelIdentifier());
        assertTrue(result.overallScore() >= 70, "Score should be strong for well-structured resume");
        assertEquals(4, result.dimensionScores().size());

        // Verify extracted data
        assertEquals("alex.morgan@example.com", result.extractedData().email());
        assertEquals("Alex Morgan", result.extractedData().candidateName());
        assertTrue(result.extractedData().quantifiableMetricsCount() >= 3);

        // Verify skills and evidence
        assertFalse(result.skillAnalysis().technicalSkills().isEmpty());
        assertTrue(result.skillAnalysis().technicalSkills().stream()
                .anyMatch(s -> s.name().equalsIgnoreCase("Java") && s.hasContext()));

        // Verify strengths and improvements
        assertFalse(result.strengths().isEmpty());
        assertNotNull(result.summary());
    }

    @Test
    void testAnalyze_JobSpecificMatching() {
        String resume = """
                Jane Engineer
                jane@example.com | New York, NY
                
                EXPERIENCE
                Software Engineer at Capital Co
                - Developed backend systems using Java and Spring Boot.
                - Designed database schemas in PostgreSQL.
                
                EDUCATION
                B.S. in Computer Science
                """;

        Job job = new Job(
                "Full-Stack Engineer",
                "FinTech Global",
                "New York, NY",
                "Looking for full-stack engineer with Java and React.",
                "FULL_TIME",
                "https://example.com/jobs/1"
        );
        job.setId(UUID.randomUUID());

        Skill javaSkill = new Skill("Java", "Backend");
        javaSkill.setId(UUID.randomUUID());
        Skill reactSkill = new Skill("React", "Frontend");
        reactSkill.setId(UUID.randomUUID());

        job.addJobSkill(javaSkill, true);
        job.addJobSkill(reactSkill, true);

        ResumeAnalysisResult result = client.analyze(resume, Optional.of(job));

        assertNotNull(result.jobMatch());
        assertEquals(job.getId(), result.jobMatch().jobId());
        assertTrue(result.jobMatch().matchedSkills().contains("Java"));
        assertTrue(result.jobMatch().missingRequiredSkills().contains("React"));
        assertEquals(50, result.jobMatch().matchScore());
    }
}
