package com.careerpilot.service;

import com.careerpilot.dto.SkillGapSummaryDto;
import com.careerpilot.entity.*;
import com.careerpilot.repository.JobRepository;
import com.careerpilot.repository.ResumeAnalysisRepository;
import com.careerpilot.repository.UserSkillRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillGapAnalysisServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @Mock
    private ResumeAnalysisRepository resumeAnalysisRepository;

    private SkillNormalizer skillNormalizer;
    private ObjectMapper objectMapper;
    private SkillGapAnalysisService skillGapAnalysisService;

    private final UUID testUserId = UUID.randomUUID();
    private Profile testProfile;

    @BeforeEach
    void setUp() {
        skillNormalizer = new SkillNormalizer();
        objectMapper = new ObjectMapper();
        skillGapAnalysisService = new SkillGapAnalysisService(
                jobRepository,
                userSkillRepository,
                resumeAnalysisRepository,
                skillNormalizer,
                objectMapper
        );

        testProfile = new Profile(testUserId, "Jane Developer", "Full Stack Engineer", "Remote", "Senior Backend Engineer");
    }

    @Test
    @DisplayName("Should detect acquired skills from profile and identify missing gaps from matching jobs")
    void testAnalyzeSkillGaps_WithProfileAndJobs() {
        // Mock user skills: Candidate has Java and PostgreSQL
        Skill javaSkill = new Skill("Java", "Backend");
        Skill pgSkill = new Skill("PostgreSQL", "Database");
        UserSkill us1 = new UserSkill(testProfile, javaSkill, "Advanced", 4);
        UserSkill us2 = new UserSkill(testProfile, pgSkill, "Intermediate", 2);

        when(userSkillRepository.findByProfileUserId(testUserId)).thenReturn(List.of(us1, us2));
        when(resumeAnalysisRepository.findFirstByUserUserIdOrderByCreatedAtDesc(testUserId)).thenReturn(Optional.empty());

        // Mock job matching "Backend Engineer"
        Job backendJob = new Job(
                "Senior Backend Engineer",
                "Cloud Scale Inc",
                "Remote",
                "Looking for a Senior Backend Engineer",
                "FULL_TIME",
                "https://example.com/job/1"
        );
        backendJob.setId(UUID.randomUUID());
        backendJob.setActive(true);

        Skill dockerSkill = new Skill("Docker", "DevOps");
        dockerSkill.setId(UUID.randomUUID());
        Skill k8sSkill = new Skill("Kubernetes", "DevOps");
        k8sSkill.setId(UUID.randomUUID());

        backendJob.addJobSkill(javaSkill, true);
        backendJob.addJobSkill(dockerSkill, true);
        backendJob.addJobSkill(k8sSkill, false);

        when(jobRepository.findAllActiveWithSkills()).thenReturn(List.of(backendJob));

        SkillGapSummaryDto summary = skillGapAnalysisService.analyzeSkillGaps(testProfile, "Senior Backend Engineer");

        assertThat(summary).isNotNull();
        assertThat(summary.acquiredSkillsCount()).isGreaterThanOrEqualTo(2);
        assertThat(summary.gapSkillsCount()).isGreaterThanOrEqualTo(2);

        // Docker and Kubernetes should be identified as gaps
        List<String> gapNames = summary.gapSkills().stream().map(g -> g.skill().toLowerCase()).toList();
        assertThat(gapNames).contains("docker", "kubernetes");

        // Java should be acquired
        List<String> acquiredNames = summary.acquiredSkills().stream().map(a -> a.skill().toLowerCase()).toList();
        assertThat(acquiredNames).contains("java");

        assertThat(summary.readinessScore()).isBetween(0, 100);
    }

    @Test
    @DisplayName("Should incorporate extracted skills from latest resume analysis")
    void testAnalyzeSkillGaps_WithResumeAnalysis() {
        when(userSkillRepository.findByProfileUserId(testUserId)).thenReturn(Collections.emptyList());

        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setSkillAnalysis("""
                {
                  "technicalSkills": [
                    { "name": "React", "category": "Frontend", "hasContext": true, "evidence": "Built responsive dashboard in React 18" }
                  ],
                  "softSkills": [],
                  "skillsWithoutEvidence": ["AWS"]
                }
                """);

        when(resumeAnalysisRepository.findFirstByUserUserIdOrderByCreatedAtDesc(testUserId)).thenReturn(Optional.of(analysis));
        when(jobRepository.findAllActiveWithSkills()).thenReturn(Collections.emptyList());

        SkillGapSummaryDto summary = skillGapAnalysisService.analyzeSkillGaps(testProfile, "Frontend Engineer");

        assertThat(summary).isNotNull();
        List<String> acquiredSkills = summary.acquiredSkills().stream().map(s -> s.skill().toLowerCase()).toList();
        assertThat(acquiredSkills).contains("react");
    }
}
