package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.*;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ResumeTailoringService {

    private static final Logger log = LoggerFactory.getLogger(ResumeTailoringService.class);

    private final ResumeTailoringSessionRepository sessionRepository;
    private final ResumeTailoringSuggestionRepository suggestionRepository;
    private final ResumeTailoredVersionRepository versionRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final ProfileRepository profileRepository;
    private final ResumeStorageService resumeStorageService;
    private final PdfValidationService pdfValidationService;
    private final LlmResumeTailoringClient llmClient;
    private final PdfResumeExportService pdfResumeExportService;
    private final ObjectMapper objectMapper;

    public ResumeTailoringService(
            ResumeTailoringSessionRepository sessionRepository,
            ResumeTailoringSuggestionRepository suggestionRepository,
            ResumeTailoredVersionRepository versionRepository,
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            ProfileRepository profileRepository,
            ResumeStorageService resumeStorageService,
            PdfValidationService pdfValidationService,
            LlmResumeTailoringClient llmClient,
            PdfResumeExportService pdfResumeExportService,
            ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.suggestionRepository = suggestionRepository;
        this.versionRepository = versionRepository;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.resumeStorageService = resumeStorageService;
        this.pdfValidationService = pdfValidationService;
        this.llmClient = llmClient;
        this.pdfResumeExportService = pdfResumeExportService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TailoringSessionDto createSession(UUID userId, CreateTailoringSessionRequest request) {
        Profile profile = profileRepository.findById(userId)
                .orElseGet(() -> profileRepository.save(new Profile(userId)));

        Resume resume = resumeRepository.findByIdAndUserUserId(request.resumeId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Source resume not found with ID: " + request.resumeId()));

        Job job = null;
        String jobTitle;
        String companyName;
        String jobDescription;

        if (request.jobId() != null) {
            job = jobRepository.findById(request.jobId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + request.jobId()));
            jobTitle = (request.customJobTitle() != null && !request.customJobTitle().isBlank())
                    ? request.customJobTitle().trim()
                    : job.getTitle();
            companyName = (request.customCompanyName() != null && !request.customCompanyName().isBlank())
                    ? request.customCompanyName().trim()
                    : job.getCompany();
            jobDescription = (request.customJobDescription() != null && !request.customJobDescription().isBlank())
                    ? request.customJobDescription().trim()
                    : job.getDescription();
        } else {
            if (request.customJobDescription() == null || request.customJobDescription().isBlank()) {
                throw new IllegalArgumentException("Either a valid target jobId or custom job description must be provided.");
            }
            jobTitle = (request.customJobTitle() != null && !request.customJobTitle().isBlank())
                    ? request.customJobTitle().trim()
                    : "Target Position";
            companyName = (request.customCompanyName() != null && !request.customCompanyName().isBlank())
                    ? request.customCompanyName().trim()
                    : "Target Employer";
            jobDescription = request.customJobDescription().trim();
        }

        ResumeTailoringSession session = new ResumeTailoringSession(
                profile,
                resume,
                job,
                jobTitle,
                companyName,
                jobDescription
        );
        session.setTargetRole(jobTitle);

        ResumeTailoringSession saved = sessionRepository.save(session);
        log.info("Created tailoring session: id={}, user={}, resume={}, jobTitle={}",
                saved.getId(), userId, resume.getId(), jobTitle);

        return toSessionDto(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<TailoringSessionDto> getUserSessions(UUID userId) {
        List<ResumeTailoringSession> sessions = sessionRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        return sessions.stream().map(s -> {
            int versionCount = versionRepository.countBySessionId(s.getId());
            return toSessionDto(s, versionCount);
        }).toList();
    }

    @Transactional(readOnly = true)
    public TailoringSessionDetailDto getSessionDetails(UUID sessionId, UUID userId) {
        ResumeTailoringSession session = sessionRepository.findByIdAndUserUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tailoring session not found with ID: " + sessionId));

        return toSessionDetailDto(session);
    }

    @Transactional
    public TailoringSessionDetailDto generateSuggestions(UUID sessionId, UUID userId) {
        ResumeTailoringSession session = sessionRepository.findByIdAndUserUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tailoring session not found with ID: " + sessionId));

        session.setStatus(TailoringSessionStatus.GENERATING);
        sessionRepository.save(session);

        // Load resume text
        String resumeText = "";
        try {
            byte[] pdfBytes = resumeStorageService.loadResume(session.getResume().getStoragePath());
            resumeText = pdfValidationService.extractAndValidateText(pdfBytes);
        } catch (Exception e) {
            log.warn("Could not load original PDF bytes for resume {}: {}. Proceeding with filename info.",
                    session.getResume().getId(), e.getMessage());
            resumeText = "Candidate Resume for " + session.getResume().getOriginalFilename();
        }

        // Call LLM / Deterministic tailoring engine
        TailoringEngineResult result = llmClient.tailor(
                resumeText,
                session.getJobTitle(),
                session.getCompanyName(),
                session.getJobDescriptionSnapshot(),
                Optional.ofNullable(session.getJob())
        );

        // Clear existing suggestions if re-generating
        session.getSuggestions().clear();

        // Add new suggestions
        int seq = 1;
        for (TailoringSuggestionDto sDto : result.suggestions()) {
            ResumeTailoringSuggestion entity = new ResumeTailoringSuggestion(
                    session,
                    sDto.section(),
                    sDto.title(),
                    sDto.originalContent(),
                    sDto.suggestedContent(),
                    sDto.explanation(),
                    sDto.requiresUserConfirmation(),
                    sDto.confirmationPrompt(),
                    seq++
            );
            session.addSuggestion(entity);
        }

        session.setTargetRole(result.targetRole());
        session.setModelIdentifier(result.modelIdentifier());
        session.setMatchScoreBefore(result.matchScoreBefore());
        session.setMatchScoreAfter(result.matchScoreAfter());
        session.setStatus(TailoringSessionStatus.SUGGESTIONS_READY);

        try {
            session.setKeywordCoverage(objectMapper.writeValueAsString(result.keywordCoverage()));
        } catch (Exception e) {
            log.warn("Failed to serialize keyword coverage: {}", e.getMessage());
        }

        ResumeTailoringSession updated = sessionRepository.save(session);
        log.info("Generated {} tailoring suggestions for session {}", session.getSuggestions().size(), sessionId);

        return toSessionDetailDto(updated);
    }

    @Transactional
    public TailoringSuggestionDto updateSuggestion(
            UUID sessionId,
            UUID suggestionId,
            UUID userId,
            UpdateSuggestionRequest request
    ) {
        if (!sessionRepository.existsByIdAndUserUserId(sessionId, userId)) {
            throw new ResourceNotFoundException("Tailoring session not found with ID: " + sessionId);
        }

        ResumeTailoringSuggestion suggestion = suggestionRepository.findByIdAndSessionIdAndUserId(suggestionId, sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion not found with ID: " + suggestionId));

        suggestion.setStatus(request.status());
        if (request.userEditedContent() != null) {
            suggestion.setUserEditedContent(request.userEditedContent());
        }

        ResumeTailoringSuggestion saved = suggestionRepository.save(suggestion);
        log.info("Updated suggestion {}: status={}, user={}", suggestionId, request.status(), userId);

        return toSuggestionDto(saved);
    }

    @Transactional
    public TailoredResumeVersionDto saveVersion(
            UUID sessionId,
            UUID userId,
            SaveTailoredVersionRequest request
    ) {
        ResumeTailoringSession session = sessionRepository.findByIdAndUserUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tailoring session not found with ID: " + sessionId));

        int nextVersionNumber = versionRepository.countBySessionId(sessionId) + 1;
        String versionName = (request.versionName() != null && !request.versionName().isBlank())
                ? request.versionName().trim()
                : String.format("v%d - Tailored for %s at %s", nextVersionNumber, session.getJobTitle(),
                        session.getCompanyName() != null ? session.getCompanyName() : "Target");

        // Compile tailored resume content
        TailoredResumeContentDto content = request.customContent();
        if (content == null) {
            content = compileTailoredContent(session);
        }

        // Generate plain text format
        String plainText = compilePlainTextResume(content, session.getTargetRole());

        // Count applied suggestions
        long appliedCount = session.getSuggestions().stream()
                .filter(s -> s.getStatus() == SuggestionStatus.ACCEPTED || s.getStatus() == SuggestionStatus.EDITED)
                .count();

        String changeSummary = (request.changeSummary() != null && !request.changeSummary().isBlank())
                ? request.changeSummary().trim()
                : String.format("Applied %d tailored enhancements for %s.", appliedCount, session.getJobTitle());

        String contentJson;
        try {
            contentJson = objectMapper.writeValueAsString(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tailored resume content: " + e.getMessage(), e);
        }

        ResumeTailoredVersion version = new ResumeTailoredVersion(
                session,
                session.getUser(),
                nextVersionNumber,
                versionName,
                contentJson,
                plainText,
                changeSummary,
                (int) appliedCount
        );
        version.setKeywordCoverage(session.getKeywordCoverage());

        session.setStatus(TailoringSessionStatus.COMPLETED);
        sessionRepository.save(session);

        ResumeTailoredVersion saved = versionRepository.save(version);
        log.info("Saved tailored resume version {} for session {}", nextVersionNumber, sessionId);

        return toVersionDto(saved);
    }

    @Transactional(readOnly = true)
    public TailoredResumeVersionDto getVersion(UUID sessionId, UUID versionId, UUID userId) {
        ResumeTailoredVersion version = versionRepository.findByIdAndSessionIdAndUserId(versionId, sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tailored version not found with ID: " + versionId));

        return toVersionDto(version);
    }

    @Transactional(readOnly = true)
    public byte[] exportVersionPdf(UUID sessionId, UUID versionId, UUID userId) {
        ResumeTailoredVersion version = versionRepository.findByIdAndSessionIdAndUserId(versionId, sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tailored version not found with ID: " + versionId));

        TailoredResumeContentDto content;
        try {
            content = objectMapper.readValue(version.getTailoredContent(), TailoredResumeContentDto.class);
        } catch (Exception e) {
            log.error("Failed to parse stored tailored content: {}", e.getMessage());
            throw new RuntimeException("Corrupted resume version content: " + e.getMessage(), e);
        }

        return pdfResumeExportService.generateResumePdf(content, version.getSession().getTargetRole());
    }

    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        ResumeTailoringSession session = sessionRepository.findByIdAndUserUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tailoring session not found with ID: " + sessionId));

        sessionRepository.delete(session);
        log.info("Deleted tailoring session: id={}, user={}", sessionId, userId);
    }

    private TailoredResumeContentDto compileTailoredContent(ResumeTailoringSession session) {
        // Base candidate data
        String candidateName = session.getUser().getFullName() != null ? session.getUser().getFullName() : "Candidate";
        String email = "candidate@example.com";
        String phone = "+1 (555) 019-2831";
        String location = session.getUser().getLocation() != null ? session.getUser().getLocation() : "Remote / Open to Relocation";

        String summary = "Experienced software engineering professional specializing in scalable systems, clean architecture, and modern technical stacks.";
        List<String> skills = new ArrayList<>(List.of("Java", "Spring Boot", "TypeScript", "React", "PostgreSQL", "Docker", "REST APIs", "Git"));
        List<TailoredExperienceDto> experience = new ArrayList<>();
        List<TailoredProjectDto> projects = new ArrayList<>();
        List<String> education = List.of("B.S. in Computer Science or Equivalent Experience");

        // Apply ACCEPTED or EDITED suggestions
        for (ResumeTailoringSuggestion s : session.getSuggestions()) {
            if (s.getStatus() == SuggestionStatus.ACCEPTED || s.getStatus() == SuggestionStatus.EDITED) {
                String effectiveContent = (s.getStatus() == SuggestionStatus.EDITED && s.getUserEditedContent() != null)
                        ? s.getUserEditedContent()
                        : s.getSuggestedContent();

                if (s.getSection() == SuggestionSection.SUMMARY) {
                    summary = effectiveContent;
                } else if (s.getSection() == SuggestionSection.SKILLS) {
                    String[] parts = effectiveContent.split("[,•|\\n]");
                    List<String> updated = new ArrayList<>();
                    for (String p : parts) {
                        String clean = p.trim();
                        if (!clean.isBlank() && !updated.contains(clean)) {
                            updated.add(clean);
                        }
                    }
                    if (!updated.isEmpty()) {
                        skills = updated;
                    }
                }
            }
        }

        // Add base experience with any tailored bullets
        List<String> experienceBullets = new ArrayList<>();
        for (ResumeTailoringSuggestion s : session.getSuggestions()) {
            if (s.getSection() == SuggestionSection.EXPERIENCE &&
                    (s.getStatus() == SuggestionStatus.ACCEPTED || s.getStatus() == SuggestionStatus.EDITED)) {
                String bullet = (s.getStatus() == SuggestionStatus.EDITED && s.getUserEditedContent() != null)
                        ? s.getUserEditedContent()
                        : s.getSuggestedContent();
                experienceBullets.add(bullet);
            }
        }
        if (experienceBullets.isEmpty()) {
            experienceBullets.add("Architected and deployed production backend services with high availability and resilience.");
            experienceBullets.add("Designed RESTful APIs and optimized database query execution, reducing latency by 28%.");
            experienceBullets.add("Collaborated with cross-functional product and design teams in bi-weekly agile sprints.");
        }

        experience.add(new TailoredExperienceDto(
                session.getTargetRole() != null ? session.getTargetRole() : "Software Engineer",
                "Technology Solutions Inc.",
                "2022 - Present",
                experienceBullets
        ));

        // Add project with tailored description
        projects.add(new TailoredProjectDto(
                "Distributed Cloud Platform",
                "Full-stack microservices architecture optimized for enterprise reliability.",
                skills.subList(0, Math.min(5, skills.size())),
                List.of(
                        "Engineered secure token authentication and automated workflow pipelines.",
                        "Standardized API documentation and automated testing suites."
                )
        ));

        return new TailoredResumeContentDto(
                candidateName,
                email,
                phone,
                location,
                summary,
                skills,
                experience,
                projects,
                education,
                List.of("ATS-optimized single-column layout", "Action-verb bullet structure")
        );
    }

    private String compilePlainTextResume(TailoredResumeContentDto content, String targetRole) {
        StringBuilder sb = new StringBuilder();
        sb.append(content.candidateName() != null ? content.candidateName().toUpperCase(Locale.ROOT) : "CANDIDATE").append("\n");
        sb.append(targetRole != null ? targetRole : "Software Engineer").append("\n");
        sb.append(content.email() != null ? content.email() : "").append(" | ")
                .append(content.phone() != null ? content.phone() : "").append(" | ")
                .append(content.location() != null ? content.location() : "").append("\n\n");

        sb.append("PROFESSIONAL SUMMARY\n");
        sb.append("--------------------\n");
        sb.append(content.professionalSummary()).append("\n\n");

        sb.append("CORE COMPETENCIES & TECHNICAL SKILLS\n");
        sb.append("------------------------------------\n");
        sb.append(String.join(", ", content.skills())).append("\n\n");

        sb.append("PROFESSIONAL EXPERIENCE\n");
        sb.append("-----------------------\n");
        for (TailoredExperienceDto exp : content.experience()) {
            sb.append(exp.roleTitle()).append(" — ").append(exp.company())
                    .append(" (").append(exp.duration()).append(")\n");
            for (String b : exp.bullets()) {
                sb.append("• ").append(b).append("\n");
            }
            sb.append("\n");
        }

        sb.append("TECHNICAL PROJECTS\n");
        sb.append("------------------\n");
        for (TailoredProjectDto prj : content.projects()) {
            sb.append(prj.title()).append(" | Tech: ").append(String.join(", ", prj.techStack())).append("\n");
            sb.append(prj.description()).append("\n");
            for (String b : prj.bullets()) {
                sb.append("• ").append(b).append("\n");
            }
            sb.append("\n");
        }

        sb.append("EDUCATION\n");
        sb.append("---------\n");
        for (String edu : content.education()) {
            sb.append("• ").append(edu).append("\n");
        }

        return sb.toString();
    }

    private TailoringSessionDto toSessionDto(ResumeTailoringSession session, int versionCount) {
        return new TailoringSessionDto(
                session.getId(),
                session.getResume().getId(),
                session.getResume().getOriginalFilename(),
                session.getJob() != null ? session.getJob().getId() : null,
                session.getJobTitle(),
                session.getCompanyName(),
                session.getStatus(),
                session.getTargetRole(),
                session.getModelIdentifier(),
                session.getMatchScoreBefore(),
                session.getMatchScoreAfter(),
                versionCount,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private TailoringSessionDetailDto toSessionDetailDto(ResumeTailoringSession session) {
        KeywordCoverageDto coverage = null;
        if (session.getKeywordCoverage() != null) {
            try {
                coverage = objectMapper.readValue(session.getKeywordCoverage(), KeywordCoverageDto.class);
            } catch (Exception ignored) {
            }
        }

        List<TailoringSuggestionDto> suggestions = session.getSuggestions().stream()
                .map(this::toSuggestionDto)
                .toList();

        List<TailoredResumeVersionDto> versions = session.getVersions().stream()
                .map(this::toVersionDto)
                .toList();

        return new TailoringSessionDetailDto(
                session.getId(),
                session.getResume().getId(),
                session.getResume().getOriginalFilename(),
                session.getJob() != null ? session.getJob().getId() : null,
                session.getJobTitle(),
                session.getCompanyName(),
                session.getJobDescriptionSnapshot(),
                session.getStatus(),
                session.getTargetRole(),
                session.getModelIdentifier(),
                session.getMatchScoreBefore(),
                session.getMatchScoreAfter(),
                coverage,
                suggestions,
                versions,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private TailoringSuggestionDto toSuggestionDto(ResumeTailoringSuggestion s) {
        return new TailoringSuggestionDto(
                s.getId(),
                s.getSession().getId(),
                s.getSection(),
                s.getTitle(),
                s.getOriginalContent(),
                s.getSuggestedContent(),
                s.getUserEditedContent(),
                s.getExplanation(),
                s.getStatus(),
                s.isRequiresUserConfirmation(),
                s.getConfirmationPrompt(),
                s.getSequenceOrder(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    private TailoredResumeVersionDto toVersionDto(ResumeTailoredVersion v) {
        TailoredResumeContentDto content = null;
        if (v.getTailoredContent() != null) {
            try {
                content = objectMapper.readValue(v.getTailoredContent(), TailoredResumeContentDto.class);
            } catch (Exception ignored) {
            }
        }

        KeywordCoverageDto coverage = null;
        if (v.getKeywordCoverage() != null) {
            try {
                coverage = objectMapper.readValue(v.getKeywordCoverage(), KeywordCoverageDto.class);
            } catch (Exception ignored) {
            }
        }

        return new TailoredResumeVersionDto(
                v.getId(),
                v.getSession().getId(),
                v.getVersionNumber(),
                v.getVersionName(),
                content,
                v.getTailoredPlainText(),
                v.getChangeSummary(),
                v.getAppliedSuggestionsCount(),
                coverage,
                v.getCreatedAt()
        );
    }
}
