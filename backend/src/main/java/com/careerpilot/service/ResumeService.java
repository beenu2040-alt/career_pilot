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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final PdfValidationService pdfValidationService;
    private final ResumeStorageService resumeStorageService;
    private final LlmResumeAnalysisClient analysisClient;
    private final ObjectMapper objectMapper;

    public ResumeService(
            ResumeRepository resumeRepository,
            ResumeAnalysisRepository resumeAnalysisRepository,
            ProfileRepository profileRepository,
            JobRepository jobRepository,
            PdfValidationService pdfValidationService,
            ResumeStorageService resumeStorageService,
            LlmResumeAnalysisClient analysisClient,
            ObjectMapper objectMapper
    ) {
        this.resumeRepository = resumeRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.pdfValidationService = pdfValidationService;
        this.resumeStorageService = resumeStorageService;
        this.analysisClient = analysisClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ResumeSummaryDto uploadResume(UUID userId, MultipartFile file) {
        pdfValidationService.validateUploadFile(file);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded resume file stream", e);
        }

        // Validate PDF extractability before saving
        pdfValidationService.extractAndValidateText(bytes);

        Profile profile = profileRepository.findById(userId)
                .orElseGet(() -> profileRepository.save(new Profile(userId)));

        UUID resumeId = UUID.randomUUID();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";
        String storagePath = resumeStorageService.storeResume(userId, resumeId, originalFilename, bytes);

        Resume resume = new Resume(
                profile,
                originalFilename,
                storagePath,
                file.getSize(),
                file.getContentType() != null ? file.getContentType() : "application/pdf"
        );
        resume.setId(resumeId);
        resume.setProcessingStatus(ResumeProcessingStatus.UPLOADED);

        Resume saved = resumeRepository.save(resume);
        log.info("Saved resume record: id={}, user={}, filename={}", saved.getId(), userId, originalFilename);

        return toSummaryDto(saved, null, null);
    }

    @Transactional(readOnly = true)
    public List<ResumeSummaryDto> getUserResumes(UUID userId) {
        List<Resume> resumes = resumeRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        List<ResumeSummaryDto> result = new ArrayList<>();

        for (Resume r : resumes) {
            Optional<ResumeAnalysis> latestAnalysis = resumeAnalysisRepository
                    .findFirstByResumeIdAndUserUserIdOrderByCreatedAtDesc(r.getId(), userId);
            Integer score = latestAnalysis.map(ResumeAnalysis::getOverallScore).orElse(null);
            UUID analysisId = latestAnalysis.map(ResumeAnalysis::getId).orElse(null);
            result.add(toSummaryDto(r, score, analysisId));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public ResumeSummaryDto getResumeById(UUID resumeId, UUID userId) {
        Resume resume = resumeRepository.findByIdAndUserUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        Optional<ResumeAnalysis> latestAnalysis = resumeAnalysisRepository
                .findFirstByResumeIdAndUserUserIdOrderByCreatedAtDesc(resumeId, userId);
        Integer score = latestAnalysis.map(ResumeAnalysis::getOverallScore).orElse(null);
        UUID analysisId = latestAnalysis.map(ResumeAnalysis::getId).orElse(null);

        return toSummaryDto(resume, score, analysisId);
    }

    @Transactional
    public void deleteResume(UUID resumeId, UUID userId) {
        Resume resume = resumeRepository.findByIdAndUserUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        String storagePath = resume.getStoragePath();
        resumeRepository.delete(resume);

        if (storagePath != null) {
            resumeStorageService.deleteResume(storagePath);
        }
        log.info("Deleted resume: id={}, user={}", resumeId, userId);
    }

    @Transactional
    public ResumeAnalysisDto analyzeResume(UUID resumeId, UUID userId, Optional<UUID> targetJobId) {
        Resume resume = resumeRepository.findByIdAndUserUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        resume.setProcessingStatus(ResumeProcessingStatus.PROCESSING);
        resumeRepository.save(resume);

        Optional<Job> targetJob = Optional.empty();
        if (targetJobId.isPresent()) {
            targetJob = jobRepository.findById(targetJobId.get());
        }

        // Load PDF bytes from storage
        byte[] pdfBytes = resumeStorageService.loadResume(resume.getStoragePath());
        String extractedText = pdfValidationService.extractAndValidateText(pdfBytes);

        // Perform Analysis
        ResumeAnalysisResult analysisResult = analysisClient.analyze(extractedText, targetJob);

        ResumeAnalysis analysis = new ResumeAnalysis(resume, resume.getUser(), targetJob.orElse(null));
        analysis.setAnalysisStatus(ResumeAnalysisStatus.COMPLETED);
        analysis.setOverallScore(analysisResult.overallScore());
        analysis.setSummary(analysisResult.summary());
        analysis.setModelIdentifier(analysisResult.modelIdentifier());
        analysis.setCompletedAt(Instant.now());

        // Serialize structured attributes
        analysis.setDimensionScores(toJson(analysisResult.dimensionScores()));
        analysis.setExtractedData(toJson(analysisResult.extractedData()));
        analysis.setStrengths(toJson(analysisResult.strengths()));
        analysis.setImprovements(toJson(analysisResult.improvements()));
        analysis.setSkillAnalysis(toJson(analysisResult.skillAnalysis()));
        analysis.setExperienceAnalysis(toJson(analysisResult.experienceAnalysis()));

        ResumeAnalysis savedAnalysis = resumeAnalysisRepository.save(analysis);

        resume.setProcessingStatus(ResumeProcessingStatus.COMPLETED);
        resumeRepository.save(resume);

        log.info("Completed analysis: id={}, resume={}, user={}, score={}",
                savedAnalysis.getId(), resumeId, userId, analysisResult.overallScore());

        return toAnalysisDto(savedAnalysis, analysisResult.jobMatch());
    }

    @Transactional(readOnly = true)
    public List<ResumeAnalysisDto> getResumeAnalyses(UUID resumeId, UUID userId) {
        if (!resumeRepository.existsByIdAndUserUserId(resumeId, userId)) {
            throw new ResourceNotFoundException("Resume not found with ID: " + resumeId);
        }

        List<ResumeAnalysis> list = resumeAnalysisRepository.findByResumeIdAndUserUserIdOrderByCreatedAtDesc(resumeId, userId);
        return list.stream().map(a -> toAnalysisDto(a, null)).toList();
    }

    @Transactional(readOnly = true)
    public ResumeAnalysisDto getResumeAnalysisById(UUID analysisId, UUID userId) {
        ResumeAnalysis analysis = resumeAnalysisRepository.findByIdAndUserUserIdWithJob(analysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume analysis not found with ID: " + analysisId));

        return toAnalysisDto(analysis, null);
    }

    private ResumeSummaryDto toSummaryDto(Resume resume, Integer latestScore, UUID latestAnalysisId) {
        return new ResumeSummaryDto(
                resume.getId(),
                resume.getOriginalFilename(),
                resume.getFileSize(),
                resume.getMimeType(),
                resume.getProcessingStatus(),
                resume.getCreatedAt(),
                resume.getUpdatedAt(),
                latestScore,
                latestAnalysisId
        );
    }

    private ResumeAnalysisDto toAnalysisDto(ResumeAnalysis entity, JobMatchAnalysisDto dynamicJobMatch) {
        List<DimensionScoreDto> dimensions = fromJson(entity.getDimensionScores(), new TypeReference<>() {}, List.of());
        ExtractedDataDto extracted = fromJson(entity.getExtractedData(), new TypeReference<>() {},
                new ExtractedDataDto("Candidate", "", "", "", List.of(), 0, 0, List.of(), List.of()));
        List<StrengthDto> strengths = fromJson(entity.getStrengths(), new TypeReference<>() {}, List.of());
        List<ImprovementDto> improvements = fromJson(entity.getImprovements(), new TypeReference<>() {}, List.of());
        SkillAnalysisDto skills = fromJson(entity.getSkillAnalysis(), new TypeReference<>() {},
                new SkillAnalysisDto(List.of(), List.of(), List.of()));
        ExperienceAnalysisDto experience = fromJson(entity.getExperienceAnalysis(), new TypeReference<>() {},
                new ExperienceAnalysisDto(0, "MODERATE", "LIMITED_METRICS", List.of()));

        JobMatchAnalysisDto jobMatch = dynamicJobMatch;
        if (jobMatch == null && entity.getJob() != null) {
            Job job = entity.getJob();
            jobMatch = new JobMatchAnalysisDto(
                    job.getId(),
                    job.getTitle(),
                    job.getCompany(),
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    List.of("Analyzed against role: " + job.getTitle() + " at " + job.getCompany())
            );
        }

        return new ResumeAnalysisDto(
                entity.getId(),
                entity.getResume().getId(),
                entity.getJob() != null ? entity.getJob().getId() : null,
                entity.getAnalysisStatus(),
                entity.getOverallScore(),
                entity.getSummary(),
                dimensions,
                extracted,
                strengths,
                improvements,
                skills,
                experience,
                jobMatch,
                entity.getModelIdentifier(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON serialization error: {}", e.getMessage());
            return "{}";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef, T fallback) {
        if (json == null || json.isBlank()) return fallback;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("JSON deserialization error: {}", e.getMessage());
            return fallback;
        }
    }
}
