package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.*;
import com.careerpilot.repository.JobRepository;
import com.careerpilot.repository.ResumeAnalysisRepository;
import com.careerpilot.repository.UserSkillRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SkillGapAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(SkillGapAnalysisService.class);

    private final JobRepository jobRepository;
    private final UserSkillRepository userSkillRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final SkillNormalizer skillNormalizer;
    private final ObjectMapper objectMapper;

    public SkillGapAnalysisService(
            JobRepository jobRepository,
            UserSkillRepository userSkillRepository,
            ResumeAnalysisRepository resumeAnalysisRepository,
            SkillNormalizer skillNormalizer,
            ObjectMapper objectMapper
    ) {
        this.jobRepository = jobRepository;
        this.userSkillRepository = userSkillRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.skillNormalizer = skillNormalizer;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SkillGapSummaryDto analyzeSkillGaps(Profile profile, String targetRole) {
        String effectiveTargetRole = (targetRole != null && !targetRole.isBlank())
                ? targetRole.trim()
                : (profile.getTargetRole() != null && !profile.getTargetRole().isBlank()
                ? profile.getTargetRole().trim()
                : "Software Engineer");

        // 1. Collect candidate's verified/acquired skills from Profile (UserSkills)
        Map<String, SkillGapDto> acquiredMap = new LinkedHashMap<>();
        List<UserSkill> userSkills = userSkillRepository.findByProfileUserId(profile.getUserId());
        for (UserSkill us : userSkills) {
            String rawName = us.getSkill() != null ? us.getSkill().getName() : null;
            if (rawName == null || rawName.isBlank()) continue;
            String normalized = skillNormalizer.normalize(rawName);
            String category = us.getSkill().getCategory() != null ? us.getSkill().getCategory() : categorizeSkill(rawName);
            String evidence = "Recorded in Profile (" + us.getProficiencyLevel()
                    + (us.getYearsExperience() != null ? ", " + us.getYearsExperience() + " yrs" : "") + ")";

            acquiredMap.put(normalized, new SkillGapDto(
                    rawName,
                    category,
                    "ACQUIRED",
                    "HIGH",
                    1,
                    evidence
            ));
        }

        // 2. Supplement candidate skills from latest Resume Analysis (if available)
        Optional<ResumeAnalysis> latestAnalysisOpt = resumeAnalysisRepository.findFirstByUserUserIdOrderByCreatedAtDesc(profile.getUserId());
        if (latestAnalysisOpt.isPresent()) {
            ResumeAnalysis analysis = latestAnalysisOpt.get();
            extractSkillsFromAnalysis(analysis, acquiredMap);
        }

        // 3. Query active jobs matching target role from database
        List<Job> activeJobs = jobRepository.findAllActiveWithSkills();
        List<Job> matchingJobs = findJobsMatchingRole(activeJobs, effectiveTargetRole);

        // Aggregate required and preferred skills from actual job requirements
        Map<String, Integer> marketSkillFrequencies = new HashMap<>();
        Map<String, String> marketSkillDisplayNames = new HashMap<>();
        Map<String, String> marketSkillCategories = new HashMap<>();
        Map<String, Boolean> marketSkillIsRequired = new HashMap<>();

        for (Job job : matchingJobs) {
            if (job.getJobSkills() == null) continue;
            for (JobSkill js : job.getJobSkills()) {
                if (js.getSkill() == null) continue;
                String rawSkill = js.getSkill().getName();
                String norm = skillNormalizer.normalize(rawSkill);
                marketSkillFrequencies.put(norm, marketSkillFrequencies.getOrDefault(norm, 0) + 1);
                marketSkillDisplayNames.putIfAbsent(norm, rawSkill);
                if (js.getSkill().getCategory() != null) {
                    marketSkillCategories.putIfAbsent(norm, js.getSkill().getCategory());
                }
                if (js.isRequired()) {
                    marketSkillIsRequired.put(norm, true);
                }
            }
        }

        // Fallback default skills if no catalog jobs matched target role
        if (marketSkillFrequencies.isEmpty()) {
            populateDefaultMarketSkills(effectiveTargetRole, marketSkillFrequencies, marketSkillDisplayNames, marketSkillCategories, marketSkillIsRequired);
        }

        int totalMatchingJobs = Math.max(matchingJobs.size(), 1);

        // 4. Divide into Acquired Skills vs Skill Gaps
        List<SkillGapDto> acquiredSkillsList = new ArrayList<>();
        List<SkillGapDto> gapSkillsList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : marketSkillFrequencies.entrySet()) {
            String norm = entry.getKey();
            int freq = entry.getValue();
            String displayName = marketSkillDisplayNames.getOrDefault(norm, capitalize(norm));
            String category = marketSkillCategories.getOrDefault(norm, categorizeSkill(displayName));
            boolean isStrictlyRequired = marketSkillIsRequired.getOrDefault(norm, false) || freq >= Math.max(1, totalMatchingJobs / 2);

            String priority = isStrictlyRequired ? "HIGH" : (freq > 1 ? "MEDIUM" : "LOW");

            if (acquiredMap.containsKey(norm)) {
                SkillGapDto existing = acquiredMap.get(norm);
                acquiredSkillsList.add(new SkillGapDto(
                        existing.skill(),
                        existing.category() != null ? existing.category() : category,
                        existing.status(),
                        priority,
                        freq,
                        existing.evidenceOrContext()
                ));
            } else {
                String gapEvidence = "Demanded in " + freq + " active " + effectiveTargetRole + " requisitions";
                gapSkillsList.add(new SkillGapDto(
                        displayName,
                        category,
                        "GAP",
                        priority,
                        freq,
                        gapEvidence
                ));
            }
        }

        // Also include candidate's other acquired skills not in the specific job listing requirements
        for (Map.Entry<String, SkillGapDto> entry : acquiredMap.entrySet()) {
            if (!marketSkillFrequencies.containsKey(entry.getKey())) {
                acquiredSkillsList.add(entry.getValue());
            }
        }

        // Sort gap skills: HIGH priority first, then by frequency descending
        gapSkillsList.sort((a, b) -> {
            int pComp = priorityRank(a.priority()) - priorityRank(b.priority());
            if (pComp != 0) return pComp;
            return Integer.compare(b.marketDemandFrequency(), a.marketDemandFrequency());
        });

        // Sort acquired skills: HIGH priority first
        acquiredSkillsList.sort((a, b) -> priorityRank(a.priority()) - priorityRank(b.priority()));

        int totalSkills = acquiredSkillsList.size() + gapSkillsList.size();
        int readinessScore = totalSkills > 0
                ? (int) Math.round(((double) acquiredSkillsList.size() / totalSkills) * 100.0)
                : 70;

        List<String> keyStrengths = acquiredSkillsList.stream()
                .limit(5)
                .map(SkillGapDto::skill)
                .collect(Collectors.toList());

        List<String> priorityGaps = gapSkillsList.stream()
                .filter(g -> "HIGH".equals(g.priority()))
                .limit(5)
                .map(SkillGapDto::skill)
                .collect(Collectors.toList());

        if (priorityGaps.isEmpty() && !gapSkillsList.isEmpty()) {
            priorityGaps = gapSkillsList.stream().limit(3).map(SkillGapDto::skill).collect(Collectors.toList());
        }

        return new SkillGapSummaryDto(
                totalSkills,
                acquiredSkillsList.size(),
                gapSkillsList.size(),
                readinessScore,
                acquiredSkillsList,
                gapSkillsList,
                keyStrengths,
                priorityGaps
        );
    }

    private void extractSkillsFromAnalysis(ResumeAnalysis analysis, Map<String, SkillGapDto> acquiredMap) {
        if (analysis == null) return;
        try {
            if (analysis.getSkillAnalysis() != null && !analysis.getSkillAnalysis().isBlank()) {
                SkillAnalysisDto skillData = objectMapper.readValue(analysis.getSkillAnalysis(), SkillAnalysisDto.class);
                if (skillData.technicalSkills() != null) {
                    for (SkillEvidenceDto se : skillData.technicalSkills()) {
                        String norm = skillNormalizer.normalize(se.name());
                        if (!acquiredMap.containsKey(norm)) {
                            String evidenceSnippet = (se.evidence() != null && !se.evidence().isBlank())
                                    ? "Resume: \"" + se.evidence().trim() + "\""
                                    : "Demonstrated in uploaded resume";
                            acquiredMap.put(norm, new SkillGapDto(
                                    se.name(),
                                    se.category() != null ? se.category() : categorizeSkill(se.name()),
                                    "ACQUIRED",
                                    "HIGH",
                                    1,
                                    evidenceSnippet
                            ));
                        }
                    }
                }
                if (skillData.skillsWithoutEvidence() != null) {
                    for (String unverified : skillData.skillsWithoutEvidence()) {
                        String norm = skillNormalizer.normalize(unverified);
                        if (!acquiredMap.containsKey(norm)) {
                            acquiredMap.put(norm, new SkillGapDto(
                                    unverified,
                                    categorizeSkill(unverified),
                                    "NEEDS_EVIDENCE",
                                    "MEDIUM",
                                    1,
                                    "Mentioned in resume without direct project metrics"
                            ));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Could not parse resume skill analysis JSON: {}", ex.getMessage());
        }
    }

    private List<Job> findJobsMatchingRole(List<Job> activeJobs, String targetRole) {
        String query = targetRole.toLowerCase(Locale.ROOT);
        String[] keywords = query.split("[\\s/-]+");

        List<Job> directMatches = activeJobs.stream()
                .filter(job -> {
                    String title = job.getTitle().toLowerCase(Locale.ROOT);
                    for (String kw : keywords) {
                        if (kw.length() > 2 && title.contains(kw)) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());

        if (!directMatches.isEmpty()) {
            return directMatches;
        }

        // If no title matches, return all active jobs so market requirements can still be computed
        return activeJobs;
    }

    private void populateDefaultMarketSkills(
            String targetRole,
            Map<String, Integer> frequencies,
            Map<String, String> names,
            Map<String, String> categories,
            Map<String, Boolean> isRequired
    ) {
        String roleLower = targetRole.toLowerCase(Locale.ROOT);
        String[][] defaults;

        if (roleLower.contains("frontend") || roleLower.contains("ui") || roleLower.contains("react")) {
            defaults = new String[][]{
                    {"TypeScript", "Frontend", "true", "4"},
                    {"React", "Frontend", "true", "4"},
                    {"Tailwind CSS", "Frontend", "false", "3"},
                    {"Next.js", "Frontend", "false", "3"},
                    {"REST API Design", "Architecture", "true", "3"},
                    {"CI/CD", "DevOps", "false", "2"},
                    {"Docker", "DevOps", "false", "2"}
            };
        } else if (roleLower.contains("cloud") || roleLower.contains("devops") || roleLower.contains("infra")) {
            defaults = new String[][]{
                    {"Docker", "DevOps", "true", "5"},
                    {"Kubernetes", "DevOps", "true", "4"},
                    {"AWS", "Cloud", "true", "5"},
                    {"CI/CD", "DevOps", "true", "4"},
                    {"Linux", "Infrastructure", "true", "4"},
                    {"Python", "Backend", "false", "3"},
                    {"System Architecture", "Architecture", "true", "3"}
            };
        } else {
            // General Backend & Full-Stack default
            defaults = new String[][]{
                    {"Java", "Backend", "true", "4"},
                    {"Spring Boot", "Backend", "true", "4"},
                    {"PostgreSQL", "Database", "true", "4"},
                    {"Docker", "DevOps", "true", "3"},
                    {"REST API Design", "Architecture", "true", "4"},
                    {"Microservices", "Architecture", "false", "3"},
                    {"AWS", "Cloud", "false", "3"},
                    {"Redis", "Database", "false", "2"},
                    {"CI/CD", "DevOps", "false", "3"}
            };
        }

        for (String[] def : defaults) {
            String norm = skillNormalizer.normalize(def[0]);
            names.put(norm, def[0]);
            categories.put(norm, def[1]);
            isRequired.put(norm, Boolean.parseBoolean(def[2]));
            frequencies.put(norm, Integer.parseInt(def[3]));
        }
    }

    private String categorizeSkill(String skillName) {
        String s = skillName.toLowerCase(Locale.ROOT);
        if (s.contains("java") || s.contains("python") || s.contains("node") || s.contains("go") || s.contains("spring") || s.contains("c#")) {
            return "Backend";
        }
        if (s.contains("react") || s.contains("vue") || s.contains("angular") || s.contains("script") || s.contains("css") || s.contains("html") || s.contains("tailwind") || s.contains("next")) {
            return "Frontend";
        }
        if (s.contains("sql") || s.contains("postgres") || s.contains("mongo") || s.contains("redis") || s.contains("database")) {
            return "Database";
        }
        if (s.contains("docker") || s.contains("k8s") || s.contains("kubernetes") || s.contains("ci/cd") || s.contains("linux")) {
            return "DevOps";
        }
        if (s.contains("aws") || s.contains("gcp") || s.contains("azure") || s.contains("cloud")) {
            return "Cloud";
        }
        if (s.contains("api") || s.contains("architecture") || s.contains("microservice") || s.contains("system")) {
            return "Architecture";
        }
        return "Technical Skills";
    }

    private int priorityRank(String priority) {
        if ("HIGH".equalsIgnoreCase(priority)) return 1;
        if ("MEDIUM".equalsIgnoreCase(priority)) return 2;
        return 3;
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
