package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.*;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class JobService {

    private final JobRepository jobRepository;
    private final SavedJobRepository savedJobRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProfileRepository profileRepository;
    private final JobMatchingService matchingService;
    private final SkillNormalizer skillNormalizer;

    public JobService(
            JobRepository jobRepository,
            SavedJobRepository savedJobRepository,
            UserSkillRepository userSkillRepository,
            ProfileRepository profileRepository,
            JobMatchingService matchingService,
            SkillNormalizer skillNormalizer
    ) {
        this.jobRepository = jobRepository;
        this.savedJobRepository = savedJobRepository;
        this.userSkillRepository = userSkillRepository;
        this.profileRepository = profileRepository;
        this.matchingService = matchingService;
        this.skillNormalizer = skillNormalizer;
    }

    /**
     * Retrieves paginated job listings with dynamic search, filters, and optional personalized match scores.
     */
    public PageResponse<JobSummaryResponse> getJobs(
            String search,
            String location,
            String employmentType,
            String remoteType,
            String experienceLevel,
            String skill,
            UUID authenticatedUserId,
            Pageable pageable
    ) {
        Specification<Job> spec = buildJobSpecification(search, location, employmentType, remoteType, experienceLevel, skill);
        Page<Job> jobsPage = jobRepository.findAll(spec, pageable);

        Set<UUID> savedJobIds = authenticatedUserId != null
                ? savedJobRepository.findSavedJobIdsByUserId(authenticatedUserId)
                : Collections.emptySet();

        List<UserSkill> userSkills = authenticatedUserId != null
                ? userSkillRepository.findByProfileUserId(authenticatedUserId)
                : Collections.emptyList();

        List<JobSummaryResponse> content = jobsPage.getContent().stream()
                .map(job -> toSummaryResponse(job, savedJobIds.contains(job.getId()), userSkills, authenticatedUserId != null))
                .toList();

        return PageResponse.of(content, jobsPage.getNumber(), jobsPage.getSize(), jobsPage.getTotalElements());
    }

    /**
     * Retrieves full job details, required/optional skills, save state, and explainable match breakdown.
     */
    public JobDetailResponse getJobDetail(UUID jobId, UUID authenticatedUserId) {
        Job job = jobRepository.findActiveWithSkillsById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job posting not found with id: " + jobId));

        boolean isSaved = false;
        JobMatchResponse matchResponse = null;

        if (authenticatedUserId != null) {
            isSaved = savedJobRepository.existsByUserUserIdAndJobId(authenticatedUserId, jobId);
            List<UserSkill> userSkills = userSkillRepository.findByProfileUserId(authenticatedUserId);
            matchResponse = matchingService.calculateMatch(job, userSkills);
        }

        return toDetailResponse(job, isSaved, matchResponse);
    }

    /**
     * Retrieves authenticated user's saved jobs.
     */
    public PageResponse<JobSummaryResponse> getSavedJobs(UUID userId, Pageable pageable) {
        Page<SavedJob> savedPage = savedJobRepository.findByUserUserId(userId, pageable);

        List<UserSkill> userSkills = userSkillRepository.findByProfileUserId(userId);

        List<JobSummaryResponse> content = savedPage.getContent().stream()
                .map(SavedJob::getJob)
                .filter(Objects::nonNull)
                .map(job -> toSummaryResponse(job, true, userSkills, true))
                .toList();

        return PageResponse.of(content, savedPage.getNumber(), savedPage.getSize(), savedPage.getTotalElements());
    }

    /**
     * Saves a job to the authenticated user's bookmarked list. Idempotent.
     */
    @Transactional
    public SaveJobResponse saveJob(UUID userId, UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if (!savedJobRepository.existsByUserUserIdAndJobId(userId, jobId)) {
            Profile profile = profileRepository.findById(userId)
                    .orElseGet(() -> profileRepository.save(new Profile(userId)));

            SavedJob savedJob = new SavedJob(profile, job);
            savedJobRepository.save(savedJob);
        }

        return new SaveJobResponse(jobId, true, "Job saved successfully");
    }

    /**
     * Removes a job from the authenticated user's saved list. Idempotent.
     */
    @Transactional
    public SaveJobResponse unsaveJob(UUID userId, UUID jobId) {
        if (savedJobRepository.existsByUserUserIdAndJobId(userId, jobId)) {
            savedJobRepository.deleteByUserUserIdAndJobId(userId, jobId);
        }
        return new SaveJobResponse(jobId, false, "Job removed from saved jobs");
    }

    /**
     * Returns personalized job recommendations for the authenticated user, ranked by match percentage.
     */
    public PageResponse<JobSummaryResponse> getRecommendations(UUID userId, Pageable pageable) {
        List<UserSkill> userSkills = userSkillRepository.findByProfileUserId(userId);
        List<Job> allActiveJobs = jobRepository.findAllActiveWithSkills();
        Set<UUID> savedJobIds = savedJobRepository.findSavedJobIdsByUserId(userId);

        // If user has 0 skills, return empty list with clean pagination
        if (userSkills.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), pageable.getPageNumber(), pageable.getPageSize(), 0);
        }

        // Calculate matches and rank
        List<ScoredJob> scoredJobs = allActiveJobs.stream()
                .map(job -> {
                    JobMatchResponse match = matchingService.calculateMatch(job, userSkills);
                    return new ScoredJob(job, match);
                })
                .sorted((a, b) -> {
                    // 1. Jobs with required skills come before unrated jobs
                    if (a.match().hasRequiredSkills() && !b.match().hasRequiredSkills()) return -1;
                    if (!a.match().hasRequiredSkills() && b.match().hasRequiredSkills()) return 1;

                    // 2. Highest match percentage first
                    int scoreA = a.match().matchPercentage() != null ? a.match().matchPercentage() : -1;
                    int scoreB = b.match().matchPercentage() != null ? b.match().matchPercentage() : -1;
                    int scoreComp = Integer.compare(scoreB, scoreA);
                    if (scoreComp != 0) return scoreComp;

                    // 3. Highest number of matched required skills
                    int countComp = Integer.compare(b.match().matchedRequiredCount(), a.match().matchedRequiredCount());
                    if (countComp != 0) return countComp;

                    // 4. Most recently posted
                    return b.job().getPostedAt().compareTo(a.job().getPostedAt());
                })
                .toList();

        // Paginate manually from sorted list
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), scoredJobs.size());

        List<JobSummaryResponse> pagedContent = (start <= end && start < scoredJobs.size())
                ? scoredJobs.subList(start, end).stream()
                .map(sj -> toSummaryResponse(sj.job(), savedJobIds.contains(sj.job().getId()), userSkills, true))
                .toList()
                : Collections.emptyList();

        return PageResponse.of(pagedContent, pageable.getPageNumber(), pageable.getPageSize(), scoredJobs.size());
    }

    /**
     * Calculates the explainable match for a single job for the authenticated user.
     */
    public JobMatchResponse getJobMatch(UUID jobId, UUID userId) {
        Job job = jobRepository.findActiveWithSkillsById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        List<UserSkill> userSkills = userSkillRepository.findByProfileUserId(userId);
        return matchingService.calculateMatch(job, userSkills);
    }

    // ------------------------------------------------------------------------
    // Helper & Specification Builders
    // ------------------------------------------------------------------------

    private Specification<Job> buildJobSpecification(
            String search,
            String location,
            String employmentType,
            String remoteType,
            String experienceLevel,
            String skill
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            // Always only active jobs in discovery
            predicates.add(cb.isTrue(root.get("isActive")));

            // Search in title, company, or description
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("company")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            // Location filter
            if (location != null && !location.trim().isEmpty()) {
                String locPattern = "%" + location.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("location")), locPattern));
            }

            // Employment Type filter
            if (employmentType != null && !employmentType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("employmentType"), employmentType.trim().toUpperCase(Locale.ROOT)));
            }

            // Remote Type filter
            if (remoteType != null && !remoteType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("remoteType"), remoteType.trim().toUpperCase(Locale.ROOT)));
            }

            // Experience Level filter
            if (experienceLevel != null && !experienceLevel.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("experienceLevel"), experienceLevel.trim().toUpperCase(Locale.ROOT)));
            }

            // Skill filter
            if (skill != null && !skill.trim().isEmpty()) {
                String canonicalSkill = skillNormalizer.canonicalize(skill);
                Join<Job, JobSkill> jobSkills = root.join("jobSkills", JoinType.INNER);
                Join<JobSkill, Skill> skillJoin = jobSkills.join("skill", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(skillJoin.get("name")), canonicalSkill));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private JobSummaryResponse toSummaryResponse(
            Job job,
            boolean isSaved,
            List<UserSkill> userSkills,
            boolean isAuthenticated
    ) {
        List<SkillDto> required = new ArrayList<>();
        List<SkillDto> optional = new ArrayList<>();

        if (job.getJobSkills() != null) {
            for (JobSkill js : job.getJobSkills()) {
                Skill s = js.getSkill();
                SkillDto dto = new SkillDto(s.getId(), s.getName(), s.getCategory(), js.isRequired());
                if (js.isRequired()) {
                    required.add(dto);
                } else {
                    optional.add(dto);
                }
            }
        }

        required.sort(Comparator.comparing(SkillDto::name, String.CASE_INSENSITIVE_ORDER));
        optional.sort(Comparator.comparing(SkillDto::name, String.CASE_INSENSITIVE_ORDER));

        Integer matchPercentage = null;
        if (isAuthenticated) {
            JobMatchResponse match = matchingService.calculateMatch(job, userSkills);
            if (match != null) {
                matchPercentage = match.matchPercentage();
            }
        }

        return new JobSummaryResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getRemoteType(),
                job.getExperienceLevel(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency(),
                job.getPostedAt(),
                job.isSample(),
                required,
                optional,
                isSaved,
                matchPercentage
        );
    }

    private JobDetailResponse toDetailResponse(Job job, boolean isSaved, JobMatchResponse match) {
        List<SkillDto> required = new ArrayList<>();
        List<SkillDto> optional = new ArrayList<>();

        if (job.getJobSkills() != null) {
            for (JobSkill js : job.getJobSkills()) {
                Skill s = js.getSkill();
                SkillDto dto = new SkillDto(s.getId(), s.getName(), s.getCategory(), js.isRequired());
                if (js.isRequired()) {
                    required.add(dto);
                } else {
                    optional.add(dto);
                }
            }
        }

        required.sort(Comparator.comparing(SkillDto::name, String.CASE_INSENSITIVE_ORDER));
        optional.sort(Comparator.comparing(SkillDto::name, String.CASE_INSENSITIVE_ORDER));

        return new JobDetailResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getDescription(),
                job.getEmploymentType(),
                job.getRemoteType(),
                job.getExperienceLevel(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency(),
                job.getSourceUrl(),
                job.getPostedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.isSample(),
                required,
                optional,
                isSaved,
                match
        );
    }

    private record ScoredJob(Job job, JobMatchResponse match) {}
}
