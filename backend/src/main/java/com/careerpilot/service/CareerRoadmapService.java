package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.*;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.CareerRoadmapRepository;
import com.careerpilot.repository.ProfileRepository;
import com.careerpilot.repository.RoadmapMilestoneRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CareerRoadmapService {

    private static final Logger log = LoggerFactory.getLogger(CareerRoadmapService.class);

    private final CareerRoadmapRepository careerRoadmapRepository;
    private final RoadmapMilestoneRepository roadmapMilestoneRepository;
    private final ProfileRepository profileRepository;
    private final SkillGapAnalysisService skillGapAnalysisService;
    private final RoadmapGeneratorService roadmapGeneratorService;
    private final ObjectMapper objectMapper;

    public CareerRoadmapService(
            CareerRoadmapRepository careerRoadmapRepository,
            RoadmapMilestoneRepository roadmapMilestoneRepository,
            ProfileRepository profileRepository,
            SkillGapAnalysisService skillGapAnalysisService,
            RoadmapGeneratorService roadmapGeneratorService,
            ObjectMapper objectMapper
    ) {
        this.careerRoadmapRepository = careerRoadmapRepository;
        this.roadmapMilestoneRepository = roadmapMilestoneRepository;
        this.profileRepository = profileRepository;
        this.skillGapAnalysisService = skillGapAnalysisService;
        this.roadmapGeneratorService = roadmapGeneratorService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RoadmapDetailDto generateRoadmap(UUID userId, GenerateRoadmapRequest request) {
        Profile profile = profileRepository.findById(userId)
                .orElseGet(() -> profileRepository.save(new Profile(userId)));

        String targetRole = (request != null && request.targetRole() != null && !request.targetRole().isBlank())
                ? request.targetRole().trim()
                : profile.getTargetRole();

        if (targetRole == null || targetRole.isBlank()) {
            throw new IllegalArgumentException("Target role must be provided in request or configured in your profile.");
        }

        String currentLevel = (request != null && request.currentLevel() != null && !request.currentLevel().isBlank())
                ? request.currentLevel().trim()
                : "Mid-Level";

        // 1. Calculate deterministic skill gaps from profile, resume analyses, and job catalog
        SkillGapSummaryDto skillGaps = skillGapAnalysisService.analyzeSkillGaps(profile, targetRole);

        // 2. Generate roadmap structure (via live AI or deterministic fallback)
        CareerRoadmap roadmap = roadmapGeneratorService.generateRoadmap(profile, targetRole, currentLevel, skillGaps);

        // 3. Persist roadmap and milestones
        CareerRoadmap saved = careerRoadmapRepository.save(roadmap);
        return mapToDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RoadmapSummaryDto> getRoadmapsForUser(UUID userId) {
        List<CareerRoadmap> list = careerRoadmapRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(this::mapToSummaryDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoadmapDetailDto getRoadmapDetail(UUID roadmapId, UUID userId) {
        CareerRoadmap roadmap = careerRoadmapRepository.findByIdAndUserUserIdWithMilestones(roadmapId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Career roadmap not found or access denied"));

        return mapToDetailDto(roadmap);
    }

    @Transactional(readOnly = true)
    public Optional<RoadmapDetailDto> getLatestRoadmap(UUID userId) {
        return careerRoadmapRepository.findFirstByUserUserIdOrderByCreatedAtDesc(userId)
                .flatMap(cr -> careerRoadmapRepository.findByIdAndUserUserIdWithMilestones(cr.getId(), userId))
                .map(this::mapToDetailDto);
    }

    @Transactional
    public RoadmapDetailDto updateMilestoneStatus(UUID roadmapId, UUID milestoneId, UUID userId, UpdateMilestoneRequest request) {
        CareerRoadmap roadmap = careerRoadmapRepository.findByIdAndUserUserIdWithMilestones(roadmapId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Career roadmap not found or access denied"));

        RoadmapMilestone milestone = roadmap.getMilestones().stream()
                .filter(m -> m.getId().equals(milestoneId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found in this roadmap"));

        MilestoneStatus newStatus;
        try {
            newStatus = MilestoneStatus.valueOf(request.status().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + request.status());
        }

        milestone.setStatus(newStatus);
        if (request.notes() != null) {
            milestone.setNotes(request.notes());
        }

        roadmap.recalculateProgress();
        CareerRoadmap saved = careerRoadmapRepository.save(roadmap);
        return mapToDetailDto(saved);
    }

    @Transactional
    public void deleteRoadmap(UUID roadmapId, UUID userId) {
        CareerRoadmap roadmap = careerRoadmapRepository.findByIdAndUserUserId(roadmapId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Career roadmap not found or access denied"));

        careerRoadmapRepository.delete(roadmap);
    }

    public RoadmapSummaryDto mapToSummaryDto(CareerRoadmap r) {
        int milestonesCount = r.getMilestones() != null ? r.getMilestones().size() : 0;
        int completedCount = r.getMilestones() != null
                ? (int) r.getMilestones().stream().filter(m -> m.getStatus() == MilestoneStatus.COMPLETED).count()
                : 0;
        int totalWeeks = r.getMilestones() != null
                ? r.getMilestones().stream().mapToInt(RoadmapMilestone::getEstimatedDurationWeeks).sum()
                : 0;

        return new RoadmapSummaryDto(
                r.getId(),
                r.getTargetRole(),
                r.getCurrentLevel(),
                r.getOverallProgress(),
                r.getStatus().name(),
                r.getSummary(),
                milestonesCount,
                completedCount,
                totalWeeks,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    public RoadmapDetailDto mapToDetailDto(CareerRoadmap r) {
        SkillGapSummaryDto skillGapSummary = null;
        if (r.getSkillGapSummary() != null && !r.getSkillGapSummary().isBlank()) {
            try {
                skillGapSummary = objectMapper.readValue(r.getSkillGapSummary(), SkillGapSummaryDto.class);
            } catch (Exception e) {
                log.debug("Could not deserialize skillGapSummary: {}", e.getMessage());
            }
        }

        List<MilestoneDto> milestoneDtos = new ArrayList<>();
        if (r.getMilestones() != null) {
            for (RoadmapMilestone m : r.getMilestones()) {
                milestoneDtos.add(mapToMilestoneDto(m));
            }
        }

        return new RoadmapDetailDto(
                r.getId(),
                r.getUser().getUserId(),
                r.getTargetRole(),
                r.getCurrentLevel(),
                r.getOverallProgress(),
                r.getStatus().name(),
                r.getSummary(),
                skillGapSummary,
                milestoneDtos,
                r.getModelIdentifier(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private MilestoneDto mapToMilestoneDto(RoadmapMilestone m) {
        List<MilestoneSkillDto> skills = List.of();
        List<MilestoneProjectDto> projects = List.of();
        List<MilestoneResourceDto> resources = List.of();
        List<String> completionCriteria = List.of();

        try {
            if (m.getSkills() != null && !m.getSkills().isBlank()) {
                skills = objectMapper.readValue(m.getSkills(), new TypeReference<List<MilestoneSkillDto>>() {});
            }
            if (m.getProjects() != null && !m.getProjects().isBlank()) {
                projects = objectMapper.readValue(m.getProjects(), new TypeReference<List<MilestoneProjectDto>>() {});
            }
            if (m.getResources() != null && !m.getResources().isBlank()) {
                resources = objectMapper.readValue(m.getResources(), new TypeReference<List<MilestoneResourceDto>>() {});
            }
            if (m.getCompletionCriteria() != null && !m.getCompletionCriteria().isBlank()) {
                completionCriteria = objectMapper.readValue(m.getCompletionCriteria(), new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.debug("Could not parse milestone JSON fields for milestone {}: {}", m.getId(), e.getMessage());
        }

        return new MilestoneDto(
                m.getId(),
                m.getRoadmap() != null ? m.getRoadmap().getId() : null,
                m.getTitle(),
                m.getDescription(),
                m.getSequenceOrder(),
                m.getStatus().name(),
                m.getEstimatedDurationWeeks(),
                m.getCompletedAt(),
                skills,
                projects,
                resources,
                completionCriteria,
                m.getNotes(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
