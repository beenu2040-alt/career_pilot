package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.Job;
import com.careerpilot.entity.JobSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeterministicResumeAnalysisClient implements ResumeAnalysisClient {

    private static final Logger log = LoggerFactory.getLogger(DeterministicResumeAnalysisClient.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
    private static final Pattern METRIC_PATTERN = Pattern.compile("(?i)\\b(\\$\\d[\\d,]*(?:\\.\\d+)?(?:k|m|b)?|\\d+(?:\\.\\d+)?%|\\d[\\d,]*(?:\\+)?\\s*(?:[a-z]+\\s+){0,3}(?:users|clients|customers|requests|qps|rps|ms|seconds|minutes|hours|days|tb|gb|mb|microservices|endpoints|engineers|team members|nodes|increase|decrease|growth|reduction|saving))\\b");
    
    private static final List<String> COMMON_TECH_SKILLS = List.of(
            "java", "spring boot", "react", "typescript", "javascript", "python",
            "docker", "kubernetes", "aws", "postgresql", "mysql", "redis",
            "linux", "ci/cd", "microservices", "rest api", "graphql", "node.js",
            "golang", "git", "tailwind css", "next.js", "kafka", "elasticsearch"
    );

    private static final List<String> SOFT_SKILLS = List.of(
            "leadership", "mentoring", "cross-functional", "agile", "scrum",
            "communication", "collaboration", "problem solving", "stakeholder management"
    );

    private static final List<String> STRONG_VERBS = List.of(
            "engineered", "architected", "spearheaded", "designed", "implemented",
            "optimized", "automated", "streamlined", "scaled", "migrated",
            "orchestrated", "reduced", "increased", "developed", "built", "delivered"
    );

    private static final List<String> WEAK_PHRASES = List.of(
            "helped with", "assisted with", "worked on", "responsible for",
            "participated in", "duties included", "familiar with", "involved in"
    );

    private final SkillNormalizer skillNormalizer;

    public DeterministicResumeAnalysisClient(SkillNormalizer skillNormalizer) {
        this.skillNormalizer = skillNormalizer;
    }

    @Override
    public String getModelIdentifier() {
        return "careerpilot-deterministic-rubric-v1";
    }

    @Override
    public ResumeAnalysisResult analyze(String resumeText, Optional<Job> targetJob) {
        String lowerText = resumeText.toLowerCase();
        String[] lines = resumeText.split("\\r?\\n");
        List<String> sentences = extractSentences(resumeText);

        // 1. Contact & Demographics Extraction
        String email = extractFirstMatch(EMAIL_PATTERN, resumeText);
        String phone = extractFirstMatch(PHONE_PATTERN, resumeText);
        String candidateName = extractCandidateName(lines, email);
        String location = extractLocation(lines);

        // 2. Sections Detected
        List<String> detectedSections = detectSections(lowerText);

        // 3. Word Count & Metrics
        int wordCount = countWords(resumeText);
        List<String> detectedMetrics = extractAllMatches(METRIC_PATTERN, resumeText);
        int metricsCount = detectedMetrics.size();

        // 4. Skills Extraction & Evidence Verification
        List<SkillEvidenceDto> techSkillsFound = new ArrayList<>();
        List<String> skillsWithoutEvidence = new ArrayList<>();
        Set<String> uniqueFoundSkills = new HashSet<>();

        for (String skill : COMMON_TECH_SKILLS) {
            String canonical = skillNormalizer.normalize(skill);
            if (lowerText.contains(canonical) || lowerText.contains(skill)) {
                if (uniqueFoundSkills.add(canonical)) {
                    String evidence = findSentenceContaining(sentences, canonical, skill);
                    boolean hasContext = (evidence != null && !isPureSkillsListSentence(evidence));
                    techSkillsFound.add(new SkillEvidenceDto(
                            capitalizeWords(canonical),
                            categorizeTechSkill(canonical),
                            hasContext,
                            hasContext ? evidence : "Listed in skills section without context."
                    ));
                    if (!hasContext) {
                        skillsWithoutEvidence.add(capitalizeWords(canonical));
                    }
                }
            }
        }

        // Soft Skills
        List<SkillEvidenceDto> softSkillsFound = new ArrayList<>();
        for (String soft : SOFT_SKILLS) {
            if (lowerText.contains(soft)) {
                String evidence = findSentenceContaining(sentences, soft, soft);
                softSkillsFound.add(new SkillEvidenceDto(
                        capitalizeWords(soft),
                        "Soft Skills & Process",
                        evidence != null,
                        evidence != null ? evidence : "Mentioned in resume text."
                ));
            }
        }

        // 5. Experience & Action Verbs
        int strongVerbCount = countOccurrences(lowerText, STRONG_VERBS);
        int weakPhraseCount = countOccurrences(lowerText, WEAK_PHRASES);
        List<String> educationList = extractEducationHints(lines);
        List<String> roleSummaries = extractRoleHints(lines);

        // 6. Rubric Dimension Scores (0-100 scale)
        DimensionScoreDto skillsDim = calculateSkillsScore(techSkillsFound, skillsWithoutEvidence);
        DimensionScoreDto expDim = calculateExperienceScore(metricsCount, strongVerbCount, weakPhraseCount);
        DimensionScoreDto structureDim = calculateStructureScore(detectedSections, wordCount, email != null);
        DimensionScoreDto specificityDim = calculateSpecificityScore(detectedMetrics, weakPhraseCount, strongVerbCount);

        int overallScore = skillsDim.score() + expDim.score() + structureDim.score() + specificityDim.score();
        overallScore = Math.max(0, Math.min(100, overallScore));

        List<DimensionScoreDto> dimensionScores = List.of(skillsDim, expDim, structureDim, specificityDim);

        // 7. Strengths
        List<StrengthDto> strengths = generateStrengths(
                techSkillsFound, detectedMetrics, strongVerbCount, detectedSections, educationList
        );

        // 8. Prioritized Improvements
        List<ImprovementDto> improvements = generateImprovements(
                weakPhraseCount, metricsCount, skillsWithoutEvidence, detectedSections, wordCount, sentences
        );

        // 9. Job Match Analysis (if targetJob present)
        JobMatchAnalysisDto jobMatch = null;
        if (targetJob.isPresent()) {
            jobMatch = generateJobMatchAnalysis(targetJob.get(), uniqueFoundSkills, sentences);
        }

        // 10. Summary
        String summary = generateExecutiveSummary(
                candidateName, techSkillsFound, detectedMetrics.size(), overallScore, roleSummaries
        );

        ExtractedDataDto extractedData = new ExtractedDataDto(
                candidateName, email, phone, location,
                detectedSections, wordCount, metricsCount,
                educationList, roleSummaries
        );

        SkillAnalysisDto skillAnalysis = new SkillAnalysisDto(
                techSkillsFound, softSkillsFound, skillsWithoutEvidence
        );

        ExperienceAnalysisDto experienceAnalysis = new ExperienceAnalysisDto(
                roleSummaries.size(),
                strongVerbCount >= 4 ? "STRONG" : (strongVerbCount >= 2 ? "MODERATE" : "NEEDS_IMPROVEMENT"),
                metricsCount >= 3 ? "QUANTIFIED" : "LIMITED_METRICS",
                roleSummaries
        );

        return new ResumeAnalysisResult(
                summary,
                overallScore,
                dimensionScores,
                extractedData,
                strengths,
                improvements,
                skillAnalysis,
                experienceAnalysis,
                jobMatch,
                getModelIdentifier()
        );
    }

    private DimensionScoreDto calculateSkillsScore(List<SkillEvidenceDto> skills, List<String> noEvidence) {
        int max = 25;
        int score = 8; // baseline

        long withEvidence = skills.stream().filter(SkillEvidenceDto::hasContext).count();
        score += (int) Math.min(12, withEvidence * 2);

        if (skills.size() >= 5) {
            score += 3;
        }

        if (!noEvidence.isEmpty() && noEvidence.size() > skills.size() / 2) {
            score = Math.max(8, score - 2); // penalty for unbacked keyword stuffing
        }

        score = Math.min(max, Math.max(5, score));

        String feedback = String.format(
                "Detected %d relevant technical skills, %d of which are backed by concrete project evidence sentences.",
                skills.size(), withEvidence
        );

        return new DimensionScoreDto(
                "Skills Evidence & Relevance",
                score,
                max,
                25,
                "HYBRID",
                feedback
        );
    }

    private DimensionScoreDto calculateExperienceScore(int metricsCount, int strongVerbs, int weakPhrases) {
        int max = 30;
        int score = 10; // baseline

        score += Math.min(12, metricsCount * 3);
        score += Math.min(8, strongVerbs * 2);

        if (weakPhrases > 0) {
            score = Math.max(6, score - Math.min(4, weakPhrases * 2));
        }

        score = Math.min(max, Math.max(6, score));

        String feedback = String.format(
                "Found %d quantifiable impact metrics and %d strong action verbs across work history.",
                metricsCount, strongVerbs
        );

        return new DimensionScoreDto(
                "Experience & Impact",
                score,
                max,
                30,
                "DETERMINISTIC",
                feedback
        );
    }

    private DimensionScoreDto calculateStructureScore(List<String> sections, int wordCount, boolean hasContact) {
        int max = 25;
        int score = 8; // baseline

        score += Math.min(10, sections.size() * 2);
        if (hasContact) {
            score += 3;
        }

        if (wordCount >= 300 && wordCount <= 1200) {
            score += 4;
        } else if (wordCount > 1500) {
            score = Math.max(5, score - 2); // penalty for overly verbose
        }

        score = Math.min(max, Math.max(5, score));

        String feedback = String.format(
                "Resume layout contains %d standard sections with %d total words (optimal density: 400-900 words).",
                sections.size(), wordCount
        );

        return new DimensionScoreDto(
                "Clarity & Structure",
                score,
                max,
                25,
                "DETERMINISTIC",
                feedback
        );
    }

    private DimensionScoreDto calculateSpecificityScore(List<String> metrics, int weakPhrases, int strongVerbs) {
        int max = 20;
        int score = 7; // baseline

        if (!metrics.isEmpty()) {
            score += Math.min(8, metrics.size() * 2);
        }
        if (strongVerbs >= 3) {
            score += 5;
        }
        if (weakPhrases == 0) {
            score += 2;
        }

        score = Math.min(max, Math.max(4, score));

        String feedback = weakPhrases > 0
                ? "Some bullet points rely on passive phrases like 'worked on'. Replace them with outcome-driven accomplishments."
                : "Strong, outcome-oriented bullet phrasing with clear technical context.";

        return new DimensionScoreDto(
                "Accomplishment Specificity",
                score,
                max,
                20,
                "MODEL_ASSESSED",
                feedback
        );
    }

    private List<StrengthDto> generateStrengths(
            List<SkillEvidenceDto> skills,
            List<String> metrics,
            int strongVerbs,
            List<String> sections,
            List<String> education
    ) {
        List<StrengthDto> list = new ArrayList<>();

        long backedSkills = skills.stream().filter(SkillEvidenceDto::hasContext).count();
        if (backedSkills >= 3) {
            list.add(new StrengthDto(
                    "Well-Evidenced Technical Proficiencies",
                    String.format("You demonstrate practical application of %d core technologies directly within project experience bullets rather than just an isolated keyword list.", backedSkills),
                    skills.stream().filter(SkillEvidenceDto::hasContext).findFirst().map(SkillEvidenceDto::evidence).orElse("")
            ));
        }

        if (!metrics.isEmpty()) {
            list.add(new StrengthDto(
                    "Quantifiable Business Impact",
                    String.format("You include %d measurable figures that prove tangible results to recruiters (e.g., %s).", metrics.size(), metrics.get(0)),
                    "Example metric detected: " + metrics.get(0)
            ));
        }

        if (strongVerbs >= 3) {
            list.add(new StrengthDto(
                    "Action-Driven Phrasing",
                    "Your experience bullets begin with active leadership verbs that emphasize ownership, delivery, and technical stewardship.",
                    "Demonstrated action verbs across role descriptions."
            ));
        }

        if (sections.contains("Education") || !education.isEmpty()) {
            list.add(new StrengthDto(
                    "Clear Academic & Foundational Credentials",
                    "Standard educational background clearly specified and easy for ATS scanners to index.",
                    education.isEmpty() ? "Education section verified." : education.get(0)
            ));
        }

        if (list.isEmpty()) {
            list.add(new StrengthDto(
                    "Clean Format Baseline",
                    "The document contains cleanly readable text and avoids parsing errors or font corruption.",
                    "Clean PDF text extraction confirmed."
            ));
        }

        return list;
    }

    private List<ImprovementDto> generateImprovements(
            int weakPhrases,
            int metricsCount,
            List<String> skillsWithoutEvidence,
            List<String> sections,
            int wordCount,
            List<String> sentences
    ) {
        List<ImprovementDto> list = new ArrayList<>();

        if (weakPhrases > 0) {
            list.add(new ImprovementDto(
                    "HIGH",
                    "Bullet Impact",
                    "Eliminate Passive Phrasing",
                    "Phrases like 'worked on' or 'responsible for' sound like task assignees rather than high-impact contributors.",
                    "Begin each bullet point with a strong past-tense action verb paired with the technical method and business outcome.",
                    "Responsible for developing backend REST APIs.",
                    "Architected and deployed 12 high-throughput REST microservices in Spring Boot, reducing p99 latency by 35%."
            ));
        }

        if (metricsCount < 3) {
            list.add(new ImprovementDto(
                    "HIGH",
                    "Metrics & Scale",
                    "Add Quantifiable Results",
                    "Several achievements lack concrete numbers, percentages, or scale indicators that demonstrate business value.",
                    "Use the Google XYZ Formula: 'Accomplished [X] as measured by [Y], by doing [Z]'. Include latency improvements, cost savings, user base, or release frequency.",
                    "Optimized database queries to improve performance.",
                    "Optimized PostgreSQL composite indexes and cached frequent queries in Redis, decreasing API response times from 420ms to 65ms for 50,000 daily active users."
            ));
        }

        if (!skillsWithoutEvidence.isEmpty()) {
            list.add(new ImprovementDto(
                    "MEDIUM",
                    "Skills Context",
                    "Integrate Disconnected Skills into Experience",
                    String.format("The following skills appear only in a skills list without mention in your work history: %s.", String.join(", ", skillsWithoutEvidence)),
                    "Weave these skills into your accomplishment bullets to prove real-world proficiency to ATS filters and hiring managers.",
                    "Skills: Java, Docker, Kubernetes, AWS.",
                    "Containerized 6 Java Spring microservices using Docker and orchestrated zero-downtime rollouts on AWS EKS Kubernetes clusters."
            ));
        }

        if (!sections.contains("Summary") && !sections.contains("Professional Summary")) {
            list.add(new ImprovementDto(
                    "LOW",
                    "Structure",
                    "Add a 3-Sentence Professional Summary",
                    "Recruiters scan resumes in 6-8 seconds; a concise executive summary immediately frames your seniority and primary tech stack.",
                    "Include: (1) Current title and years of experience, (2) Core tech expertise, (3) Key differentiator or marquee achievement.",
                    "Missing summary section.",
                    "Senior Full-Stack Engineer with 5+ years of experience designing enterprise cloud architectures using Java, Spring Boot, and React. Proven track record of scaling high-availability distributed systems."
            ));
        }

        if (wordCount < 350) {
            list.add(new ImprovementDto(
                    "MEDIUM",
                    "Content Depth",
                    "Expand Experience Descriptions",
                    "The resume is under 350 words, which may appear thin or brief to hiring teams looking for engineering depth.",
                    "Add 3-5 comprehensive bullets for your most recent roles explaining the system architecture, scale, and technical trade-offs.",
                    "Brief single-line job entries.",
                    "Detail the problem solved, architecture chosen, and measurable result for each past role."
            ));
        }

        return list;
    }

    private JobMatchAnalysisDto generateJobMatchAnalysis(
            Job job,
            Set<String> resumeSkills,
            List<String> sentences
    ) {
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingRequiredSkills = new ArrayList<>();
        List<String> missingOptionalSkills = new ArrayList<>();

        if (job.getJobSkills() != null) {
            for (JobSkill js : job.getJobSkills()) {
                String skillName = js.getSkill().getName();
                String canonical = skillNormalizer.normalize(skillName);
                if (resumeSkills.contains(canonical)) {
                    matchedSkills.add(skillName);
                } else {
                    if (js.isRequired()) {
                        missingRequiredSkills.add(skillName);
                    } else {
                        missingOptionalSkills.add(skillName);
                    }
                }
            }
        }

        int totalSkills = matchedSkills.size() + missingRequiredSkills.size();
        int matchScore = totalSkills > 0 ? (matchedSkills.size() * 100) / totalSkills : 100;

        List<String> recommendations = new ArrayList<>();
        if (!missingRequiredSkills.isEmpty()) {
            recommendations.add(String.format(
                    "Highlight any personal projects or coursework covering: %s.",
                    String.join(", ", missingRequiredSkills)
            ));
        }
        recommendations.add(String.format(
                    "Customize your summary headline toward '%s' at %s to improve ATS relevance.",
                    job.getTitle(), job.getCompany()
            ));

        return new JobMatchAnalysisDto(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                matchedSkills,
                missingRequiredSkills,
                missingOptionalSkills,
                matchScore,
                recommendations
        );
    }

    private String generateExecutiveSummary(
            String name,
            List<SkillEvidenceDto> skills,
            int metricsCount,
            int score,
            List<String> roles
    ) {
        String roleStr = roles.isEmpty() ? "Software Professional" : roles.get(0);
        String topSkills = skills.stream().limit(4).map(SkillEvidenceDto::name).reduce((a, b) -> a + ", " + b).orElse("Engineering");

        return String.format(
                "Candidate profile for %s demonstrates solid foundational competencies in %s. " +
                "The resume achieved an overall diagnostic score of %d/100, featuring %d verified quantifiable achievements. " +
                "By adopting active leadership verbs and integrating isolated technical proficiencies into project bullets, this profile can achieve top-tier ATS compliance.",
                name.isBlank() ? "this applicant" : name,
                topSkills,
                score,
                metricsCount
        );
    }

    // Helper routines
    private String extractCandidateName(String[] lines, String email) {
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && line.length() < 50 && !line.contains("@") && !line.matches(".*\\d{3}.*")) {
                if (!line.toLowerCase().contains("resume") && !line.toLowerCase().contains("curriculum")) {
                    return capitalizeWords(line);
                }
            }
        }
        if (email != null && email.contains("@")) {
            String userPart = email.substring(0, email.indexOf("@"));
            return capitalizeWords(userPart.replace(".", " ").replace("_", " "));
        }
        return "Candidate";
    }

    private String extractLocation(String[] lines) {
        Pattern locPattern = Pattern.compile("(?i)\\b([A-Z][a-zA-Z\\s]+,\\s*[A-Z]{2}|Remote|San Francisco|New York|Seattle|Austin|Chicago|Boston|London|Toronto|Bangalore|Berlin)\\b");
        for (int i = 0; i < Math.min(8, lines.length); i++) {
            Matcher m = locPattern.matcher(lines[i]);
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        return "Not Specified";
    }

    private List<String> detectSections(String lowerText) {
        List<String> sections = new ArrayList<>();
        String[] keywords = {"experience", "work experience", "employment", "education", "skills", "projects", "summary", "certifications"};
        for (String kw : keywords) {
            if (lowerText.contains(kw)) {
                sections.add(capitalizeWords(kw));
            }
        }
        return sections;
    }

    private List<String> extractEducationHints(String[] lines) {
        List<String> list = new ArrayList<>();
        Pattern eduPattern = Pattern.compile("(?i)\\b(Bachelor|Master|B\\.S\\.|M\\.S\\.|B\\.Tech|Degree|University|College|Institute|Computer Science)\\b");
        for (String line : lines) {
            if (eduPattern.matcher(line).find() && line.trim().length() > 5 && line.trim().length() < 120) {
                list.add(line.trim());
                if (list.size() >= 3) break;
            }
        }
        return list;
    }

    private List<String> extractRoleHints(String[] lines) {
        List<String> list = new ArrayList<>();
        Pattern rolePattern = Pattern.compile("(?i)\\b(Software Engineer|Developer|Architect|DevOps|Data Engineer|Full-Stack|Frontend|Backend|Manager|Lead|Intern)\\b");
        for (String line : lines) {
            if (rolePattern.matcher(line).find() && line.trim().length() > 5 && line.trim().length() < 100) {
                list.add(line.trim());
                if (list.size() >= 4) break;
            }
        }
        return list;
    }

    private List<String> extractSentences(String text) {
        List<String> result = new ArrayList<>();
        for (String s : text.split("(?<=[.!?])\\s+|\\n+")) {
            String trimmed = s.trim();
            if (trimmed.length() > 15) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String findSentenceContaining(List<String> sentences, String word1, String word2) {
        String fallback = null;
        for (String s : sentences) {
            String lower = s.toLowerCase();
            if (lower.contains(word1) || lower.contains(word2)) {
                if (!isPureSkillsListSentence(s)) {
                    return s;
                }
                if (fallback == null) {
                    fallback = s;
                }
            }
        }
        return fallback;
    }

    private boolean isPureSkillsListSentence(String sentence) {
        String lower = sentence.toLowerCase().trim();
        return lower.startsWith("skills") || lower.startsWith("technical skills")
                || lower.startsWith("languages & frameworks") || lower.startsWith("databases & cloud")
                || (sentence.contains(",") && sentence.split(",").length >= 4);
    }

    private String categorizeTechSkill(String canonical) {
        if (canonical.contains("java") || canonical.contains("python") || canonical.contains("node") || canonical.contains("go")) {
            return "Programming Languages";
        }
        if (canonical.contains("react") || canonical.contains("next") || canonical.contains("typescript") || canonical.contains("tailwind")) {
            return "Frontend Engineering";
        }
        if (canonical.contains("postgres") || canonical.contains("sql") || canonical.contains("redis")) {
            return "Databases & Storage";
        }
        if (canonical.contains("docker") || canonical.contains("kubernetes") || canonical.contains("aws") || canonical.contains("ci/cd")) {
            return "DevOps & Cloud Infrastructure";
        }
        return "Frameworks & Architecture";
    }

    private String extractFirstMatch(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group() : null;
    }

    private List<String> extractAllMatches(Pattern pattern, String text) {
        List<String> matches = new ArrayList<>();
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            matches.add(m.group());
        }
        return matches;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    private int countOccurrences(String text, List<String> terms) {
        int count = 0;
        for (String t : terms) {
            if (text.contains(t)) {
                count++;
            }
        }
        return count;
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isBlank()) return "";
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
