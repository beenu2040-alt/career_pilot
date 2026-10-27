package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class RoadmapGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(RoadmapGeneratorService.class);

    @Value("${careerpilot.ai.provider:fallback}")
    private String aiProvider;

    @Value("${careerpilot.ai.api-key:}")
    private String apiKey;

    @Value("${careerpilot.ai.model:gemini-1.5-flash}")
    private String modelName;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public RoadmapGeneratorService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isLiveAiConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.contains("your-")
                && ("gemini".equalsIgnoreCase(aiProvider) || "openai".equalsIgnoreCase(aiProvider));
    }

    public String getModelIdentifier() {
        if (isLiveAiConfigured()) {
            return aiProvider.toLowerCase(Locale.ROOT) + ":" + modelName;
        }
        return "deterministic-v5";
    }

    public CareerRoadmap generateRoadmap(Profile profile, String targetRole, String currentLevel, SkillGapSummaryDto skillGaps) {
        String effectiveLevel = (currentLevel != null && !currentLevel.isBlank()) ? currentLevel.trim() : "Mid-Level";
        CareerRoadmap roadmap = new CareerRoadmap(profile, targetRole, effectiveLevel);
        roadmap.setModelIdentifier(getModelIdentifier());

        try {
            roadmap.setSkillGapSummary(objectMapper.writeValueAsString(skillGaps));
        } catch (Exception e) {
            log.warn("Failed to serialize skillGapSummary: {}", e.getMessage());
        }

        // Attempt live AI roadmap generation if configured
        if (isLiveAiConfigured()) {
            try {
                boolean success = generateWithLiveAi(roadmap, profile, targetRole, effectiveLevel, skillGaps);
                if (success && !roadmap.getMilestones().isEmpty()) {
                    roadmap.recalculateProgress();
                    return roadmap;
                }
            } catch (Exception e) {
                log.warn("Live LLM roadmap generation failed ({}). Failing over to deterministic heuristic engine.", e.getMessage());
            }
        }

        // High-quality deterministic fallback
        generateDeterministicRoadmap(roadmap, profile, targetRole, effectiveLevel, skillGaps);
        roadmap.recalculateProgress();
        return roadmap;
    }

    private boolean generateWithLiveAi(
            CareerRoadmap roadmap,
            Profile profile,
            String targetRole,
            String currentLevel,
            SkillGapSummaryDto skillGaps
    ) throws Exception {
        String prompt = buildPrompt(profile, targetRole, currentLevel, skillGaps);

        String jsonResponse = null;
        if ("gemini".equalsIgnoreCase(aiProvider)) {
            jsonResponse = callGeminiApi(prompt);
        } else if ("openai".equalsIgnoreCase(aiProvider)) {
            jsonResponse = callOpenAiApi(prompt);
        }

        if (jsonResponse == null || jsonResponse.isBlank()) {
            return false;
        }

        JsonNode root = objectMapper.readTree(jsonResponse);
        if (!root.has("summary") || !root.has("milestones")) {
            return false;
        }

        roadmap.setSummary(root.path("summary").asText());
        JsonNode milestonesNode = root.path("milestones");

        int seq = 1;
        for (JsonNode mNode : milestonesNode) {
            RoadmapMilestone milestone = new RoadmapMilestone();
            milestone.setTitle(mNode.path("title").asText("Milestone " + seq));
            milestone.setDescription(mNode.path("description").asText(""));
            milestone.setSequenceOrder(mNode.path("sequenceOrder").asInt(seq));
            milestone.setEstimatedDurationWeeks(Math.max(1, mNode.path("estimatedDurationWeeks").asInt(3)));
            milestone.setStatus(MilestoneStatus.NOT_STARTED);

            if (mNode.has("skills")) {
                milestone.setSkills(objectMapper.writeValueAsString(mNode.path("skills")));
            }
            if (mNode.has("projects")) {
                milestone.setProjects(objectMapper.writeValueAsString(mNode.path("projects")));
            }
            if (mNode.has("resources")) {
                milestone.setResources(objectMapper.writeValueAsString(mNode.path("resources")));
            }
            if (mNode.has("completionCriteria")) {
                milestone.setCompletionCriteria(objectMapper.writeValueAsString(mNode.path("completionCriteria")));
            }

            roadmap.addMilestone(milestone);
            seq++;
        }

        return !roadmap.getMilestones().isEmpty();
    }

    private String callGeminiApi(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contentObj = Map.of("parts", List.of(textPart));
        Map<String, Object> generationConfig = Map.of(
                "response_mime_type", "application/json",
                "temperature", 0.2
        );

        Map<String, Object> payload = Map.of(
                "contents", List.of(contentObj),
                "generationConfig", generationConfig
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
        String response = restTemplate.postForObject(url, requestEntity, String.class);

        JsonNode responseJson = objectMapper.readTree(response);
        return responseJson.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();
    }

    private String callOpenAiApi(String prompt) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        Map<String, Object> systemMsg = Map.of("role", "system", "content", "You are an expert technical career architect. Output ONLY valid JSON matching the requested schema.");
        Map<String, Object> userMsg = Map.of("role", "user", "content", prompt);

        Map<String, Object> payload = Map.of(
                "model", modelName != null && !modelName.isBlank() ? modelName : "gpt-4o-mini",
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(systemMsg, userMsg)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
        String response = restTemplate.postForObject(url, requestEntity, String.class);

        JsonNode responseJson = objectMapper.readTree(response);
        return responseJson.path("choices").get(0)
                .path("message").path("content").asText();
    }

    private String buildPrompt(Profile profile, String targetRole, String currentLevel, SkillGapSummaryDto gaps) {
        return """
                Act as a Principal Staff Engineer and Senior Career Coach.
                Create a customized, realistic career advancement roadmap for a developer targeting the following objective:

                Candidate Profile:
                - Target Role: %s
                - Experience Level: %s
                - Current Strengths: %s
                - Identified Skill Gaps: %s

                Requirements:
                1. Structure 3 to 4 sequential milestones leading up to complete job readiness.
                2. Every milestone must have:
                   - title
                   - description
                   - sequenceOrder (1, 2, 3...)
                   - estimatedDurationWeeks (integer between 2 and 6)
                   - skills: array of objects with keys { "name": string, "priority": "HIGH"|"MEDIUM"|"LOW", "rationale": string }
                   - projects: array of practical project objects with keys { "title": string, "description": string, "deliverable": string, "techStack": string[] }
                   - resources: array of verified learning resources with keys { "title": string, "type": "DOCUMENTATION"|"TUTORIAL"|"GUIDE", "url": string } (Must use real, official documentation URLs such as spring.io, react.dev, docs.docker.com, postgresql.org, aws.amazon.com)
                   - completionCriteria: array of concrete strings verifying mastery.
                3. Return a root JSON object with:
                   - "summary": A personalized 2-3 sentence strategic roadmap summary.
                   - "milestones": The array of milestone objects.
                """.formatted(
                targetRole,
                currentLevel,
                gaps.keyStrengths() != null ? String.join(", ", gaps.keyStrengths()) : "Software Development",
                gaps.priorityGaps() != null ? String.join(", ", gaps.priorityGaps()) : "Architecture, DevOps"
        );
    }

    public void generateDeterministicRoadmap(
            CareerRoadmap roadmap,
            Profile profile,
            String targetRole,
            String currentLevel,
            SkillGapSummaryDto skillGaps
    ) {
        List<String> gaps = (skillGaps.priorityGaps() != null && !skillGaps.priorityGaps().isEmpty())
                ? skillGaps.priorityGaps()
                : List.of("Distributed Systems", "Cloud Infrastructure", "CI/CD Automation");

        List<String> strengths = (skillGaps.keyStrengths() != null && !skillGaps.keyStrengths().isEmpty())
                ? skillGaps.keyStrengths()
                : List.of("Core Programming", "Database Fundamentals");

        String summary = String.format(
                "Personalized roadmap to bridge %d identified skill gaps for the %s position (%s tier). " +
                        "Builds upon your proven foundation in %s with hands-on system architecture, automation, and real-world project deliverables.",
                skillGaps.gapSkillsCount(),
                targetRole,
                currentLevel,
                String.join(", ", strengths.subList(0, Math.min(2, strengths.size())))
        );
        roadmap.setSummary(summary);

        // Milestone 1: Core Competency & Foundational Gaps
        RoadmapMilestone m1 = new RoadmapMilestone(
                roadmap,
                "Phase 1: Core Framework Mastery & High-Priority Gaps",
                "Solidify essential architectural design patterns, type safety, and the core missing technologies required by top employers.",
                1,
                3
        );
        List<MilestoneSkillDto> m1Skills = new ArrayList<>();
        if (!gaps.isEmpty()) {
            m1Skills.add(new MilestoneSkillDto(gaps.get(0), "HIGH", "Directly required in market job postings for " + targetRole));
        }
        m1Skills.add(new MilestoneSkillDto("REST API Design & Clean Architecture", "HIGH", "Foundation for enterprise software engineering"));

        List<MilestoneProjectDto> m1Projects = List.of(
                new MilestoneProjectDto(
                        "Resilient API Backend Service",
                        "Build an idiomatic RESTful service featuring input validation, centralized error handling, and unit test suites.",
                        "Clean GitHub repository with 85%+ test coverage and OpenAPI / Swagger documentation",
                        List.of("REST API", "Database", "Unit Testing")
                )
        );

        List<MilestoneResourceDto> m1Resources = List.of(
                new MilestoneResourceDto("Official Spring Framework Guides", "DOCUMENTATION", "https://spring.io/guides"),
                new MilestoneResourceDto("MDN Web Developer Documentation", "DOCUMENTATION", "https://developer.mozilla.org/"),
                new MilestoneResourceDto("PostgreSQL Official Tutorial", "DOCUMENTATION", "https://www.postgresql.org/docs/current/tutorial.html")
        );

        List<String> m1Criteria = List.of(
                "Design and document RESTful endpoints with consistent schema contracts",
                "Write isolated unit and integration tests with verified assertions",
                "Demonstrate proficiency in core language idiomatic error handling"
        );

        serializeMilestoneFields(m1, m1Skills, m1Projects, m1Resources, m1Criteria);
        roadmap.addMilestone(m1);

        // Milestone 2: Cloud Infrastructure & Containerization
        RoadmapMilestone m2 = new RoadmapMilestone(
                roadmap,
                "Phase 2: Containerization & Cloud Infrastructure",
                "Package applications into reproducible OCI containers, orchestrate local multi-service environments, and configure cloud fundamentals.",
                2,
                4
        );
        List<MilestoneSkillDto> m2Skills = new ArrayList<>();
        if (gaps.size() > 1) {
            m2Skills.add(new MilestoneSkillDto(gaps.get(1), "HIGH", "Key infrastructure gap for modern deployments"));
        } else {
            m2Skills.add(new MilestoneSkillDto("Docker", "HIGH", "Standard enterprise containerization"));
        }
        m2Skills.add(new MilestoneSkillDto("Cloud Deployment & Networking", "MEDIUM", "Essential for production hosting"));

        List<MilestoneProjectDto> m2Projects = List.of(
                new MilestoneProjectDto(
                        "Multi-Container Deployment Pipeline",
                        "Containerize a web service with a multi-stage Dockerfile and configure a database with volume persistence via Compose.",
                        "Containerized image with non-root user security and local multi-service composition file",
                        List.of("Docker", "Container Compose", "Networking")
                )
        );

        List<MilestoneResourceDto> m2Resources = List.of(
                new MilestoneResourceDto("Docker Documentation & Best Practices", "DOCUMENTATION", "https://docs.docker.com/get-started/"),
                new MilestoneResourceDto("AWS Cloud Fundamentals & Getting Started", "DOCUMENTATION", "https://docs.aws.amazon.com/"),
                new MilestoneResourceDto("Kubernetes Basics & Tutorials", "DOCUMENTATION", "https://kubernetes.io/docs/tutorials/")
        );

        List<String> m2Criteria = List.of(
                "Create minimal, secure multi-stage container builds",
                "Configure persistent volumes, healthchecks, and environment secret injection",
                "Verify service-to-service communication over isolated virtual networks"
        );

        serializeMilestoneFields(m2, m2Skills, m2Projects, m2Resources, m2Criteria);
        roadmap.addMilestone(m2);

        // Milestone 3: CI/CD Automation & System Reliability
        RoadmapMilestone m3 = new RoadmapMilestone(
                roadmap,
                "Phase 3: CI/CD Automation, Caching & Performance",
                "Automate software quality checks on every pull request, implement caching layers, and optimize database indexing.",
                3,
                3
        );
        List<MilestoneSkillDto> m3Skills = new ArrayList<>();
        if (gaps.size() > 2) {
            m3Skills.add(new MilestoneSkillDto(gaps.get(2), "MEDIUM", "Differentiator for senior engineering roles"));
        } else {
            m3Skills.add(new MilestoneSkillDto("CI/CD Automation", "HIGH", "Standard continuous delivery"));
        }
        m3Skills.add(new MilestoneSkillDto("Redis Caching & Query Optimization", "MEDIUM", "Crucial for system scalability"));

        List<MilestoneProjectDto> m3Projects = List.of(
                new MilestoneProjectDto(
                        "Automated CI/CD Workflow & Cache Layer",
                        "Configure a continuous integration pipeline running linting, test suites, and Docker builds on pull requests.",
                        "GitHub Actions workflow file and benchmark report verifying sub-10ms cache retrieval",
                        List.of("GitHub Actions", "Redis", "Performance Profiling")
                )
        );

        List<MilestoneResourceDto> m3Resources = List.of(
                new MilestoneResourceDto("GitHub Actions Documentation", "DOCUMENTATION", "https://docs.github.com/en/actions"),
                new MilestoneResourceDto("Redis Official Documentation", "DOCUMENTATION", "https://redis.io/docs/"),
                new MilestoneResourceDto("Martin Fowler on Continuous Integration", "GUIDE", "https://martinfowler.com/articles/continuousIntegration.html")
        );

        List<String> m3Criteria = List.of(
                "Automate branch test verification and build gating",
                "Implement distributed caching with appropriate eviction and TTL policies",
                "Measure query performance before and after database index tuning"
        );

        serializeMilestoneFields(m3, m3Skills, m3Projects, m3Resources, m3Criteria);
        roadmap.addMilestone(m3);

        // Milestone 4: Production Capstone & Interview Portfolio
        RoadmapMilestone m4 = new RoadmapMilestone(
                roadmap,
                "Phase 4: Production Capstone & System Design Readiness",
                "Build and deploy an end-to-end portfolio system demonstrating full target role competencies for technical interviews.",
                4,
                4
        );
        List<MilestoneSkillDto> m4Skills = List.of(
                new MilestoneSkillDto("System Architecture & Scalability", "HIGH", "Core topic in technical interview rounds"),
                new MilestoneSkillDto("Production Observability & Monitoring", "MEDIUM", "Shows enterprise readiness")
        );

        List<MilestoneProjectDto> m4Projects = List.of(
                new MilestoneProjectDto(
                        "Production Portfolio Capstone",
                        "Deploy a full-stack, authenticated SaaS application with automated deployment, monitoring, and comprehensive architecture docs.",
                        "Live deployed web application link with public GitHub repository and architectural design document",
                        List.of("Full Stack Architecture", "Auth", "Observability", "Deployment")
                )
        );

        List<MilestoneResourceDto> m4Resources = List.of(
                new MilestoneResourceDto("System Design Primer by Donne Martin", "GUIDE", "https://github.com/donnemartin/system-design-primer"),
                new MilestoneResourceDto("12 Factor App Methodology", "GUIDE", "https://12factor.net/"),
                new MilestoneResourceDto("OWASP Top 10 Security Verification", "DOCUMENTATION", "https://owasp.org/www-project-top-ten/")
        );

        List<String> m4Criteria = List.of(
                "Host live working application on a public URL with SSL certificate",
                "Prepare concise architectural diagram explaining trade-offs and data flow",
                "Deliver articulate walkthrough of code structure and design decisions"
        );

        serializeMilestoneFields(m4, m4Skills, m4Projects, m4Resources, m4Criteria);
        roadmap.addMilestone(m4);
    }

    private void serializeMilestoneFields(
            RoadmapMilestone milestone,
            List<MilestoneSkillDto> skills,
            List<MilestoneProjectDto> projects,
            List<MilestoneResourceDto> resources,
            List<String> criteria
    ) {
        try {
            milestone.setSkills(objectMapper.writeValueAsString(skills));
            milestone.setProjects(objectMapper.writeValueAsString(projects));
            milestone.setResources(objectMapper.writeValueAsString(resources));
            milestone.setCompletionCriteria(objectMapper.writeValueAsString(criteria));
        } catch (Exception e) {
            log.error("Failed to serialize milestone fields", e);
        }
    }
}
