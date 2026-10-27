package com.careerpilot.service;

import com.careerpilot.dto.JobMatchResponse;
import com.careerpilot.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobMatchingServiceTest {

    private JobMatchingService matchingService;
    private SkillNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new SkillNormalizer();
        matchingService = new JobMatchingService(normalizer);
    }

    private Job createJobWithSkills(String[][] requiredSkills, String[][] optionalSkills) {
        Job job = new Job("Test Role", "Acme Corp", "Remote", "Description", "FULL_TIME", "http://example.com");
        job.setId(UUID.randomUUID());

        if (requiredSkills != null) {
            for (String[] s : requiredSkills) {
                Skill skill = new Skill(s[0], s.length > 1 ? s[1] : "Tech");
                skill.setId(UUID.randomUUID());
                job.addJobSkill(skill, true);
            }
        }

        if (optionalSkills != null) {
            for (String[] s : optionalSkills) {
                Skill skill = new Skill(s[0], s.length > 1 ? s[1] : "Tech");
                skill.setId(UUID.randomUUID());
                job.addJobSkill(skill, false);
            }
        }

        return job;
    }

    private List<UserSkill> createUserSkills(String... skillNames) {
        Profile profile = new Profile(UUID.randomUUID());
        List<UserSkill> list = new ArrayList<>();
        for (String name : skillNames) {
            Skill skill = new Skill(name, "Tech");
            skill.setId(UUID.randomUUID());
            list.add(new UserSkill(profile, skill, "ADVANCED", 3));
        }
        return list;
    }

    @Test
    @DisplayName("Should compute 100% match when all required skills are present")
    void testFullMatch() {
        Job job = createJobWithSkills(
                new String[][]{{"Java"}, {"Spring Boot"}, {"TypeScript"}, {"React"}},
                new String[][]{{"Docker"}}
        );
        List<UserSkill> userSkills = createUserSkills("Java", "Spring Boot", "TypeScript", "React");

        JobMatchResponse match = matchingService.calculateMatch(job, userSkills);

        assertTrue(match.hasRequiredSkills());
        assertEquals(100, match.matchPercentage());
        assertEquals(4, match.matchedRequiredCount());
        assertEquals(4, match.totalRequiredCount());
        assertEquals(0, match.missingRequiredSkills().size());
        assertTrue(match.explanation().contains("100%"));
    }

    @Test
    @DisplayName("Should resolve aliases during matching (e.g. JS -> JavaScript, k8s -> Kubernetes)")
    void testAliasMatch() {
        Job job = createJobWithSkills(
                new String[][]{{"JavaScript"}, {"Kubernetes"}, {"PostgreSQL"}},
                null
        );
        // User has aliases: "JS", "k8s", "postgres"
        List<UserSkill> userSkills = createUserSkills("JS", "k8s", "postgres");

        JobMatchResponse match = matchingService.calculateMatch(job, userSkills);

        assertTrue(match.hasRequiredSkills());
        assertEquals(100, match.matchPercentage());
        assertEquals(3, match.matchedRequiredCount());
        assertEquals(0, match.missingRequiredSkills().size());
    }

    @Test
    @DisplayName("Should accurately calculate partial matches and identify missing skills")
    void testPartialMatch() {
        Job job = createJobWithSkills(
                new String[][]{{"Java"}, {"Spring Boot"}, {"Docker"}, {"AWS"}},
                new String[][]{{"Kubernetes"}}
        );
        // User has 2 out of 4 required: Java and Docker, plus optional Kubernetes
        List<UserSkill> userSkills = createUserSkills("Java", "Docker", "k8s");

        JobMatchResponse match = matchingService.calculateMatch(job, userSkills);

        assertTrue(match.hasRequiredSkills());
        assertEquals(50, match.matchPercentage());
        assertEquals(2, match.matchedRequiredCount());
        assertEquals(4, match.totalRequiredCount());

        assertTrue(match.matchedRequiredSkills().contains("Java"));
        assertTrue(match.matchedRequiredSkills().contains("Docker"));

        assertTrue(match.missingRequiredSkills().contains("Spring Boot"));
        assertTrue(match.missingRequiredSkills().contains("AWS"));

        assertEquals(1, match.matchedOptionalCount());
        assertTrue(match.matchedOptionalSkills().contains("Kubernetes"));

        assertTrue(match.explanation().contains("50%"));
        assertTrue(match.explanation().contains("Missing:"));
    }

    @Test
    @DisplayName("Should handle jobs with NO specified required skills separately without misleading 100%")
    void testJobWithNoRequiredSkills() {
        Job job = createJobWithSkills(null, new String[][]{{"Docker"}});
        List<UserSkill> userSkills = createUserSkills("Java", "Spring Boot");

        JobMatchResponse match = matchingService.calculateMatch(job, userSkills);

        assertFalse(match.hasRequiredSkills());
        assertNull(match.matchPercentage(), "Match percentage must be null when job has no required skills");
        assertEquals(0, match.matchedRequiredCount());
        assertEquals(0, match.totalRequiredCount());
        assertTrue(match.explanation().contains("does not specify mandatory skill requirements"));
    }

    @Test
    @DisplayName("Should gracefully handle empty user skill profile with 0% and informative explanation")
    void testEmptyUserSkillProfile() {
        Job job = createJobWithSkills(
                new String[][]{{"Java"}, {"Spring Boot"}},
                null
        );
        List<UserSkill> emptyUserSkills = List.of();

        JobMatchResponse match = matchingService.calculateMatch(job, emptyUserSkills);

        assertTrue(match.hasRequiredSkills());
        assertEquals(0, match.matchPercentage());
        assertEquals(0, match.matchedRequiredCount());
        assertEquals(2, match.totalRequiredCount());
        assertEquals(2, match.missingRequiredSkills().size());
        assertTrue(match.explanation().contains("haven't cataloged any skills in your profile yet"));
    }
}
