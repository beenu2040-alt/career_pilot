package com.careerpilot.service;

import com.careerpilot.dto.JobMatchResponse;
import com.careerpilot.entity.Job;
import com.careerpilot.entity.JobSkill;
import com.careerpilot.entity.UserSkill;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic explainable job matching engine.
 * Computes skill alignment percentages and generates human-readable explanations.
 */
@Service
public class JobMatchingService {

    private final SkillNormalizer normalizer;

    public JobMatchingService(SkillNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /**
     * Calculates the deterministic explainable match between a set of user skills and a job posting.
     */
    public JobMatchResponse calculateMatch(Job job, List<UserSkill> userSkills) {
        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null");
        }

        // Canonical set of user's skills
        Set<String> userCanonicalSkills = (userSkills == null || userSkills.isEmpty())
                ? Collections.emptySet()
                : userSkills.stream()
                .filter(us -> us != null && us.getSkill() != null && us.getSkill().getName() != null)
                .map(us -> normalizer.canonicalize(us.getSkill().getName()))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        // Partition job skills into required vs optional
        Set<JobSkill> allJobSkills = job.getJobSkills() != null ? job.getJobSkills() : Collections.emptySet();

        List<JobSkill> requiredJobSkills = allJobSkills.stream()
                .filter(JobSkill::isRequired)
                .sorted(Comparator.comparing(js -> js.getSkill().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<JobSkill> optionalJobSkills = allJobSkills.stream()
                .filter(js -> !js.isRequired())
                .sorted(Comparator.comparing(js -> js.getSkill().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        int totalRequired = requiredJobSkills.size();
        int totalOptional = optionalJobSkills.size();

        // 1. Evaluate Required Skills
        List<String> matchedRequired = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();

        for (JobSkill js : requiredJobSkills) {
            String skillName = js.getSkill().getName();
            String canonical = normalizer.canonicalize(skillName);
            if (userCanonicalSkills.contains(canonical)) {
                matchedRequired.add(skillName);
            } else {
                missingRequired.add(skillName);
            }
        }

        // 2. Evaluate Optional Skills
        List<String> matchedOptional = new ArrayList<>();
        for (JobSkill js : optionalJobSkills) {
            String skillName = js.getSkill().getName();
            String canonical = normalizer.canonicalize(skillName);
            if (userCanonicalSkills.contains(canonical)) {
                matchedOptional.add(skillName);
            }
        }

        // 3. Score & Explanation Calculation
        if (totalRequired == 0) {
            // Case A: Job does not specify required skills.
            // Do NOT assign a misleading 100% or 0% score.
            String explanation = "This role does not specify mandatory skill requirements. A deterministic skill match score cannot be computed. Please review the job description for qualifications.";
            return new JobMatchResponse(
                    job.getId(),
                    false,
                    null,
                    0,
                    0,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    matchedOptional.size(),
                    totalOptional,
                    matchedOptional,
                    explanation
            );
        }

        // Case B: User has no skills cataloged
        if (userCanonicalSkills.isEmpty()) {
            String explanation = "You haven't cataloged any skills in your profile yet. Add your verified skills in My Profile to calculate match percentages for this role.";
            return new JobMatchResponse(
                    job.getId(),
                    true,
                    0,
                    0,
                    totalRequired,
                    Collections.emptyList(),
                    missingRequired,
                    0,
                    totalOptional,
                    Collections.emptyList(),
                    explanation
            );
        }

        // Case C: Standard calculation: (matchedRequired / totalRequired) * 100
        int percentage = (int) Math.round(((double) matchedRequired.size() / (double) totalRequired) * 100.0);
        String explanation = buildExplanation(percentage, matchedRequired, missingRequired, totalRequired, matchedOptional, totalOptional);

        return new JobMatchResponse(
                job.getId(),
                true,
                percentage,
                matchedRequired.size(),
                totalRequired,
                matchedRequired,
                missingRequired,
                matchedOptional.size(),
                totalOptional,
                matchedOptional,
                explanation
        );
    }

    private String buildExplanation(
            int percentage,
            List<String> matchedRequired,
            List<String> missingRequired,
            int totalRequired,
            List<String> matchedOptional,
            int totalOptional
    ) {
        StringBuilder sb = new StringBuilder();

        if (percentage == 100) {
            sb.append("Outstanding alignment: You meet 100% of the required skills (")
              .append(matchedRequired.size()).append("/").append(totalRequired).append(").");
        } else if (percentage >= 70) {
            sb.append("Strong match: You satisfy ").append(matchedRequired.size())
              .append(" of ").append(totalRequired).append(" required skills (")
              .append(percentage).append("%). Missing: ").append(String.join(", ", missingRequired)).append(".");
        } else if (percentage > 0) {
            sb.append("Moderate match: You satisfy ").append(matchedRequired.size())
              .append(" of ").append(totalRequired).append(" required skills (")
              .append(percentage).append("%). Missing: ").append(String.join(", ", missingRequired)).append(".");
        } else {
            sb.append("Low alignment: None of the ").append(totalRequired)
              .append(" required skills match your current profile. Missing: ")
              .append(String.join(", ", missingRequired)).append(".");
        }

        if (!matchedOptional.isEmpty()) {
            sb.append(" You also bring ").append(matchedOptional.size())
              .append(" bonus optional skill").append(matchedOptional.size() > 1 ? "s: " : ": ")
              .append(String.join(", ", matchedOptional)).append(".");
        }

        return sb.toString();
    }
}
