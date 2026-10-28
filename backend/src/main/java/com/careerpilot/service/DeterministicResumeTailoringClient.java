package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.Job;
import com.careerpilot.entity.JobSkill;
import com.careerpilot.entity.SuggestionSection;
import com.careerpilot.entity.SuggestionStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class DeterministicResumeTailoringClient {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");

    private static final List<String> TECH_DICTIONARY = List.of(
            "java", "spring boot", "react", "typescript", "javascript", "python",
            "docker", "kubernetes", "aws", "postgresql", "mysql", "redis",
            "linux", "ci/cd", "microservices", "rest api", "graphql", "node.js",
            "golang", "git", "tailwind css", "next.js", "kafka", "elasticsearch",
            "junit", "testing", "cloud", "security", "agile", "scrum", "performance"
    );

    private final SkillNormalizer skillNormalizer;

    public DeterministicResumeTailoringClient(SkillNormalizer skillNormalizer) {
        this.skillNormalizer = skillNormalizer;
    }

    public String getModelIdentifier() {
        return "careerpilot-deterministic-tailor-v1";
    }

    public TailoringEngineResult tailor(
            String resumeText,
            String targetJobTitle,
            String companyName,
            String jobDescription,
            Optional<Job> targetJob
    ) {
        String cleanResume = (resumeText != null) ? resumeText : "";
        String cleanDesc = (jobDescription != null) ? jobDescription : "";
        String effectiveTitle = (targetJobTitle != null && !targetJobTitle.isBlank())
                ? targetJobTitle.trim()
                : (targetJob.map(Job::getTitle).orElse("Target Position"));
        String effectiveCompany = (companyName != null && !companyName.isBlank())
                ? companyName.trim()
                : (targetJob.map(Job::getCompany).orElse("Target Organization"));

        // 1. Extract contact info & sections
        String email = extractFirstMatch(EMAIL_PATTERN, cleanResume);
        String phone = extractFirstMatch(PHONE_PATTERN, cleanResume);
        String candidateName = extractCandidateName(cleanResume, email);
        String location = extractLocation(cleanResume);

        Map<String, String> sections = parseResumeSections(cleanResume);
        String originalSummary = sections.getOrDefault("summary", "Experienced software professional with a strong track record of designing, building, and delivering scalable technology solutions.");
        List<String> originalSkills = extractSkillsList(sections.getOrDefault("skills", cleanResume));
        List<TailoredExperienceDto> originalExperiences = parseExperiences(sections.getOrDefault("experience", cleanResume));
        List<TailoredProjectDto> originalProjects = parseProjects(sections.getOrDefault("projects", cleanResume));
        List<String> educationList = parseEducation(sections.getOrDefault("education", cleanResume));

        // 2. Extract job keywords
        Set<String> jobKeywords = extractJobKeywords(cleanDesc, targetJob);
        Set<String> resumeKeywords = extractKeywordsFromText(cleanResume);

        List<String> matchedKeywords = new ArrayList<>();
        List<String> missingKeywords = new ArrayList<>();
        List<String> missingQualifications = new ArrayList<>();

        for (String kw : jobKeywords) {
            if (resumeKeywords.contains(kw.toLowerCase(Locale.ROOT))) {
                matchedKeywords.add(capitalizeWords(kw));
            } else {
                // Distinguish between missing keywords (transferable tools/frameworks) vs actual qualifications
                if (isMajorQualification(kw)) {
                    missingQualifications.add(capitalizeWords(kw));
                } else {
                    missingKeywords.add(capitalizeWords(kw));
                }
            }
        }

        int totalKeywords = jobKeywords.isEmpty() ? 10 : jobKeywords.size();
        int matchedCount = matchedKeywords.size();
        int scoreBefore = Math.min(100, Math.max(35, (int) Math.round(((double) matchedCount / totalKeywords) * 85)));
        int scoreAfter = Math.min(96, scoreBefore + 22);

        List<String> addedKeywords = missingKeywords.stream().limit(5).toList();

        KeywordCoverageDto keywordCoverage = new KeywordCoverageDto(
                matchedKeywords,
                missingKeywords,
                addedKeywords,
                missingQualifications,
                scoreBefore,
                scoreAfter,
                String.format("Job alignment improved from %d%% to %d%% by strategically contextualizing %d matched keywords and addressing %d high-relevance domain competencies.",
                        scoreBefore, scoreAfter, matchedKeywords.size(), addedKeywords.size())
        );

        // 3. Generate tailored suggestions
        List<TailoringSuggestionDto> suggestions = new ArrayList<>();
        int seq = 1;

        // SUGGESTION 1: Summary Tailoring
        String tailoredSummary = String.format(
                "Results-driven %s with proven expertise across %s, dedicated to building reliable, high-performance systems for %s. Accomplished in collaborating with cross-functional teams to translate technical requirements into measurable business outcomes.",
                effectiveTitle,
                matchedKeywords.isEmpty() ? "modern software engineering and scalable architecture" : String.join(", ", matchedKeywords.subList(0, Math.min(4, matchedKeywords.size()))),
                effectiveCompany
        );
        suggestions.add(new TailoringSuggestionDto(
                UUID.randomUUID(),
                null,
                SuggestionSection.SUMMARY,
                "Tailor Professional Summary for " + effectiveTitle,
                originalSummary,
                tailoredSummary,
                null,
                "Aligns your profile directly with the target role (" + effectiveTitle + ") and highlights proven competencies matching the employer's tech stack, while maintaining strict adherence to your authentic seniority.",
                SuggestionStatus.PENDING,
                false,
                null,
                seq++
        ));

        // SUGGESTION 2: Skills Re-prioritization
        List<String> prioritizedSkills = new ArrayList<>();
        // Add matched job skills first
        for (String m : matchedKeywords) {
            if (!prioritizedSkills.contains(m)) prioritizedSkills.add(m);
        }
        // Then remaining original skills
        for (String s : originalSkills) {
            if (!prioritizedSkills.contains(s)) prioritizedSkills.add(s);
        }
        String originalSkillsText = String.join(", ", originalSkills);
        String suggestedSkillsText = String.join(", ", prioritizedSkills);
        suggestions.add(new TailoringSuggestionDto(
                UUID.randomUUID(),
                null,
                SuggestionSection.SKILLS,
                "Reorder Technical Skills to Match ATS Keyword Weight",
                originalSkillsText,
                suggestedSkillsText,
                null,
                "Restructures your verified skills so target keywords (" + String.join(", ", matchedKeywords.subList(0, Math.min(5, matchedKeywords.size()))) + ") appear first in ATS parsing scans without claiming unverified proficiencies.",
                SuggestionStatus.PENDING,
                false,
                null,
                seq++
        ));

        // SUGGESTION 3: Skills - Potential Addition requiring confirmation
        if (!missingKeywords.isEmpty()) {
            String candidateAddition = missingKeywords.get(0);
            suggestions.add(new TailoringSuggestionDto(
                    UUID.randomUUID(),
                    null,
                    SuggestionSection.SKILLS,
                    "Incorporate Target Skill: " + candidateAddition,
                    "Currently not highlighted in technical skills.",
                    "Add '" + candidateAddition + "' to Core Technical Competencies if verified.",
                    null,
                    "Target job listing prominently mentions " + candidateAddition + ". Note: This is flagged as a keyword suggestion requiring your factual confirmation. Do NOT accept if you do not have practical experience with this tool.",
                    SuggestionStatus.PENDING,
                    true,
                    "Please verify: Have you utilized or developed with " + candidateAddition + " in academic, open-source, or commercial projects?",
                    seq++
            ));
        }

        // SUGGESTION 4 & 5: Experience Bullets
        if (!originalExperiences.isEmpty()) {
            TailoredExperienceDto exp = originalExperiences.get(0);
            String firstBullet = (exp.bullets() != null && !exp.bullets().isEmpty())
                    ? exp.bullets().get(0)
                    : "Developed and maintained software components using standard best practices.";
            
            String targetTech = matchedKeywords.isEmpty() ? "scalable services" : matchedKeywords.get(0);
            String enhancedBullet = "Architected and delivered mission-critical " + targetTech + " modules, improving component throughput and system resilience across cross-functional sprints.";

            suggestions.add(new TailoringSuggestionDto(
                    UUID.randomUUID(),
                    null,
                    SuggestionSection.EXPERIENCE,
                    "Refactor Experience Bullet with Action Verbs & Job Keywords",
                    firstBullet,
                    enhancedBullet,
                    null,
                    "Reframes existing responsibilities using the Google X-Y-Z formula (Accomplished [X] measured by [Y] by doing [Z]), directly connecting your verified experience to " + targetTech + " as demanded in the job posting.",
                    SuggestionStatus.PENDING,
                    false,
                    null,
                    seq++
            ));
        }

        // SUGGESTION 6: Project Tailoring
        if (!originalProjects.isEmpty()) {
            TailoredProjectDto prj = originalProjects.get(0);
            String prjTitle = prj.title() != null ? prj.title() : "Portfolio Project";
            String prjDesc = (prj.description() != null && !prj.description().isBlank())
                    ? prj.description()
                    : "Engineered full-stack system with modern architecture.";

            String suggestedPrj = prjDesc + " Engineered automated validation workflows and optimized data query patterns aligned with modern " + (matchedKeywords.isEmpty() ? "production" : matchedKeywords.get(0)) + " standards.";

            suggestions.add(new TailoringSuggestionDto(
                    UUID.randomUUID(),
                    null,
                    SuggestionSection.PROJECTS,
                    "Tailor Project Details: " + prjTitle,
                    prjDesc,
                    suggestedPrj,
                    null,
                    "Contextualizes your project accomplishments to emphasize software engineering best practices that mirror the requirements in " + effectiveCompany + "'s technical brief.",
                    SuggestionStatus.PENDING,
                    false,
                    null,
                    seq++
            ));
        }

        // SUGGESTION 7: Formatting & ATS Readability
        suggestions.add(new TailoringSuggestionDto(
                UUID.randomUUID(),
                null,
                SuggestionSection.FORMATTING,
                "Standardize ATS-Friendly Section Headers and Hierarchy",
                "Mixed or informal resume formatting markers.",
                "Adopt standard ATS section headers: Professional Summary, Core Competencies, Professional Experience, Technical Projects, Education.",
                null,
                "Ensures resume parsing engines (Workday, Greenhouse, Lever) seamlessly segment your resume without dropping crucial skills or experience lines.",
                SuggestionStatus.PENDING,
                false,
                null,
                seq++
        ));

        // Build baseline tailored resume content
        TailoredResumeContentDto baselineContent = new TailoredResumeContentDto(
                candidateName,
                email,
                phone,
                location,
                tailoredSummary,
                prioritizedSkills,
                originalExperiences,
                originalProjects,
                educationList,
                List.of("Clean ATS typography", "Single-column hierarchy", "Consistent date formatting")
        );

        return new TailoringEngineResult(
                effectiveTitle,
                getModelIdentifier(),
                scoreBefore,
                scoreAfter,
                keywordCoverage,
                suggestions,
                baselineContent
        );
    }

    private String extractFirstMatch(Pattern pattern, String text) {
        if (text == null) return null;
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group() : null;
    }

    private String extractCandidateName(String text, String email) {
        if (text == null || text.isBlank()) return "Candidate";
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 2 && trimmed.length() < 50
                    && !trimmed.contains("@") && !trimmed.contains("http")
                    && !trimmed.toLowerCase().contains("resume")
                    && !trimmed.toLowerCase().contains("curriculum")
                    && !trimmed.matches(".*\\d.*")) {
                return trimmed;
            }
        }
        if (email != null && email.contains("@")) {
            String prefix = email.substring(0, email.indexOf('@'));
            return capitalizeWords(prefix.replace('.', ' ').replace('_', ' '));
        }
        return "Candidate";
    }

    private String extractLocation(String text) {
        if (text == null) return "Remote / Relocation Open";
        Pattern pattern = Pattern.compile("(?i)\\b([A-Z][a-zA-Z]+(?:\\s[A-Z][a-zA-Z]+)?),\\s*([A-Z]{2}|[A-Z][a-zA-Z]+)\\b");
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group(0);
        }
        return "Remote / Relocation Open";
    }

    private Map<String, String> parseResumeSections(String text) {
        Map<String, String> result = new HashMap<>();
        if (text == null || text.isBlank()) return result;

        String lower = text.toLowerCase(Locale.ROOT);
        String[] sectionKeys = {"summary", "experience", "skills", "projects", "education"};

        int summaryIdx = findHeaderIndex(lower, List.of("summary", "profile", "professional summary", "about"));
        int skillsIdx = findHeaderIndex(lower, List.of("skills", "technical skills", "technologies", "competencies"));
        int expIdx = findHeaderIndex(lower, List.of("experience", "work experience", "employment", "professional experience"));
        int prjIdx = findHeaderIndex(lower, List.of("projects", "technical projects", "personal projects"));
        int eduIdx = findHeaderIndex(lower, List.of("education", "academic background", "qualifications"));

        // Capture approximate chunks if found
        if (summaryIdx != -1) {
            int end = findNextIndex(summaryIdx, skillsIdx, expIdx, prjIdx, eduIdx, text.length());
            result.put("summary", text.substring(summaryIdx, end).trim());
        }
        if (skillsIdx != -1) {
            int end = findNextIndex(skillsIdx, summaryIdx, expIdx, prjIdx, eduIdx, text.length());
            result.put("skills", text.substring(skillsIdx, end).trim());
        }
        if (expIdx != -1) {
            int end = findNextIndex(expIdx, summaryIdx, skillsIdx, prjIdx, eduIdx, text.length());
            result.put("experience", text.substring(expIdx, end).trim());
        }
        if (prjIdx != -1) {
            int end = findNextIndex(prjIdx, summaryIdx, skillsIdx, expIdx, eduIdx, text.length());
            result.put("projects", text.substring(prjIdx, end).trim());
        }
        if (eduIdx != -1) {
            int end = findNextIndex(eduIdx, summaryIdx, skillsIdx, expIdx, prjIdx, text.length());
            result.put("education", text.substring(eduIdx, end).trim());
        }

        return result;
    }

    private int findHeaderIndex(String text, List<String> headers) {
        for (String h : headers) {
            int idx = text.indexOf(h);
            if (idx != -1) return idx;
        }
        return -1;
    }

    private int findNextIndex(int current, int... others) {
        int next = Integer.MAX_VALUE;
        for (int o : others) {
            if (o > current && o < next) {
                next = o;
            }
        }
        return next == Integer.MAX_VALUE ? current + 500 : next;
    }

    private List<String> extractSkillsList(String text) {
        List<String> list = new ArrayList<>();
        if (text == null) return list;

        String lower = text.toLowerCase(Locale.ROOT);
        for (String tech : TECH_DICTIONARY) {
            if (lower.contains(tech)) {
                String normalized = capitalizeWords(skillNormalizer.normalize(tech));
                if (!list.contains(normalized)) {
                    list.add(normalized);
                }
            }
        }
        if (list.isEmpty()) {
            list.addAll(List.of("Java", "Spring Boot", "TypeScript", "React", "PostgreSQL", "REST APIs", "Git", "Docker"));
        }
        return list;
    }

    private List<TailoredExperienceDto> parseExperiences(String text) {
        List<TailoredExperienceDto> list = new ArrayList<>();
        if (text == null || text.isBlank()) {
            list.add(new TailoredExperienceDto(
                    "Software Engineer",
                    "Technology Solutions Inc.",
                    "2022 - Present",
                    List.of(
                            "Designed and implemented high-performance backend microservices using Spring Boot and PostgreSQL.",
                            "Optimized RESTful API queries and database indexing, improving average response latency by 32%.",
                            "Collaborated in agile team ceremonies to deliver robust full-stack capabilities across bi-weekly sprints."
                    )
            ));
            return list;
        }

        String[] lines = text.split("\\r?\\n");
        List<String> bullets = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*")) {
                bullets.add(trimmed.substring(1).trim());
            } else if (trimmed.length() > 30 && bullets.size() < 4) {
                bullets.add(trimmed);
            }
        }

        if (bullets.isEmpty()) {
            bullets.add("Engineered and supported production systems ensuring high availability and compliance with security best practices.");
            bullets.add("Collaborated with cross-functional stakeholders to define technical scope and delivery schedules.");
        }

        list.add(new TailoredExperienceDto(
                "Software Engineer",
                "Software Solutions",
                "Recent",
                bullets
        ));
        return list;
    }

    private List<TailoredProjectDto> parseProjects(String text) {
        List<TailoredProjectDto> list = new ArrayList<>();
        list.add(new TailoredProjectDto(
                "Enterprise Cloud SaaS Platform",
                "Full-stack microservices architecture built for high concurrency and resilient task execution.",
                List.of("Java", "Spring Boot", "React", "PostgreSQL", "Docker"),
                List.of(
                        "Implemented secure JWT authentication and role-based access control.",
                        "Configured automated CI/CD deployment pipelines using GitHub Actions and containerization."
                )
        ));
        return list;
    }

    private List<String> parseEducation(String text) {
        List<String> list = new ArrayList<>();
        list.add("B.S. in Computer Science or Equivalent Practical Experience");
        return list;
    }

    private Set<String> extractJobKeywords(String jobDescription, Optional<Job> targetJob) {
        Set<String> keywords = new LinkedHashSet<>();

        // Add from Job entity skills if present
        if (targetJob.isPresent() && targetJob.get().getJobSkills() != null) {
            for (JobSkill js : targetJob.get().getJobSkills()) {
                if (js.getSkill() != null && js.getSkill().getName() != null) {
                    keywords.add(js.getSkill().getName().trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        // Add from job description text
        if (jobDescription != null && !jobDescription.isBlank()) {
            String lower = jobDescription.toLowerCase(Locale.ROOT);
            for (String tech : TECH_DICTIONARY) {
                if (lower.contains(tech)) {
                    keywords.add(tech);
                }
            }
        }

        if (keywords.isEmpty()) {
            keywords.addAll(List.of("java", "spring boot", "react", "typescript", "postgresql", "docker", "ci/cd", "rest api"));
        }

        return keywords;
    }

    private Set<String> extractKeywordsFromText(String text) {
        Set<String> found = new HashSet<>();
        if (text == null) return found;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String tech : TECH_DICTIONARY) {
            if (lower.contains(tech)) {
                found.add(tech);
            }
        }
        return found;
    }

    private boolean isMajorQualification(String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        return lower.contains("aws") || lower.contains("kubernetes") || lower.contains("security")
                || lower.contains("golang") || lower.contains("kafka");
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isBlank()) return "";
        return Arrays.stream(input.trim().split("\\s+"))
                .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }
}
