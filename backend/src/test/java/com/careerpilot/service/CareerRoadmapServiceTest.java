package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.*;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.CareerRoadmapRepository;
import com.careerpilot.repository.ProfileRepository;
import com.careerpilot.repository.RoadmapMilestoneRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareerRoadmapServiceTest {

    @Mock
    private CareerRoadmapRepository careerRoadmapRepository;

    @Mock
    private RoadmapMilestoneRepository roadmapMilestoneRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private SkillGapAnalysisService skillGapAnalysisService;

    private RoadmapGeneratorService roadmapGeneratorService;
    private ObjectMapper objectMapper;
    private CareerRoadmapService careerRoadmapService;

    private final UUID testUserId = UUID.randomUUID();
    private Profile testProfile;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        roadmapGeneratorService = new RoadmapGeneratorService(objectMapper);
        careerRoadmapService = new CareerRoadmapService(
                careerRoadmapRepository,
                roadmapMilestoneRepository,
                profileRepository,
                skillGapAnalysisService,
                roadmapGeneratorService,
                objectMapper
        );

        testProfile = new Profile(testUserId, "Alice Tech", "Lead Developer", "NYC", "Platform Architect");
    }

    @Test
    @DisplayName("Should generate a career roadmap using profile target role and calculate initial 0% progress")
    void testGenerateRoadmap_SuccessWithProfileTargetRole() {
        when(profileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));

        SkillGapSummaryDto mockGaps = new SkillGapSummaryDto(
                5, 2, 3, 40,
                List.of(new SkillGapDto("Java", "Backend", "ACQUIRED", "HIGH", 3, "Profile")),
                List.of(new SkillGapDto("Kubernetes", "DevOps", "GAP", "HIGH", 4, "Market")),
                List.of("Java"),
                List.of("Kubernetes")
        );

        when(skillGapAnalysisService.analyzeSkillGaps(testProfile, "Platform Architect")).thenReturn(mockGaps);
        when(careerRoadmapRepository.save(any(CareerRoadmap.class))).thenAnswer(inv -> {
            CareerRoadmap rm = inv.getArgument(0);
            rm.setId(UUID.randomUUID());
            return rm;
        });

        GenerateRoadmapRequest request = new GenerateRoadmapRequest(null, "Senior");
        RoadmapDetailDto result = careerRoadmapService.generateRoadmap(testUserId, request);

        assertThat(result).isNotNull();
        assertThat(result.targetRole()).isEqualTo("Platform Architect");
        assertThat(result.currentLevel()).isEqualTo("Senior");
        assertThat(result.overallProgress()).isEqualTo(0);
        assertThat(result.status()).isEqualTo("NOT_STARTED");
        assertThat(result.milestones()).hasSize(4);
        assertThat(result.milestones().get(0).title()).contains("Phase 1");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when target role is missing in both request and profile")
    void testGenerateRoadmap_ThrowsWhenTargetRoleMissing() {
        testProfile.setTargetRole(null);
        when(profileRepository.findById(testUserId)).thenReturn(Optional.of(testProfile));

        GenerateRoadmapRequest emptyRequest = new GenerateRoadmapRequest(null, null);

        assertThatThrownBy(() -> careerRoadmapService.generateRoadmap(testUserId, emptyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target role must be provided");
    }

    @Test
    @DisplayName("Should update milestone status, timestamp, and recompute overall roadmap progress")
    void testUpdateMilestoneStatus_RecalculatesOverallProgress() {
        UUID roadmapId = UUID.randomUUID();
        UUID milestoneId1 = UUID.randomUUID();
        UUID milestoneId2 = UUID.randomUUID();

        CareerRoadmap roadmap = new CareerRoadmap(testProfile, "Staff Engineer", "Senior");
        roadmap.setId(roadmapId);

        RoadmapMilestone m1 = new RoadmapMilestone(roadmap, "Milestone 1", "Desc", 1, 2);
        m1.setId(milestoneId1);
        RoadmapMilestone m2 = new RoadmapMilestone(roadmap, "Milestone 2", "Desc", 2, 2);
        m2.setId(milestoneId2);

        roadmap.addMilestone(m1);
        roadmap.addMilestone(m2);

        when(careerRoadmapRepository.findByIdAndUserUserIdWithMilestones(roadmapId, testUserId))
                .thenReturn(Optional.of(roadmap));
        when(careerRoadmapRepository.save(any(CareerRoadmap.class))).thenAnswer(inv -> inv.getArgument(0));

        // Complete milestone 1: out of 2 milestones, 1 completed = 50%
        UpdateMilestoneRequest updateReq = new UpdateMilestoneRequest("COMPLETED", "Mastered distributed consensus");
        RoadmapDetailDto updated = careerRoadmapService.updateMilestoneStatus(roadmapId, milestoneId1, testUserId, updateReq);

        assertThat(updated.overallProgress()).isEqualTo(50);
        assertThat(updated.status()).isEqualTo("IN_PROGRESS");

        MilestoneDto updatedM1 = updated.milestones().stream().filter(m -> m.id().equals(milestoneId1)).findFirst().orElseThrow();
        assertThat(updatedM1.status()).isEqualTo("COMPLETED");
        assertThat(updatedM1.completedAt()).isNotNull();
        assertThat(updatedM1.notes()).isEqualTo("Mastered distributed consensus");
    }

    @Test
    @DisplayName("Should enforce cross-user access isolation and throw ResourceNotFoundException for foreign roadmaps")
    void testRoadmapAccessControl_ThrowsWhenRoadmapBelongsToAnotherUser() {
        UUID roadmapId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        when(careerRoadmapRepository.findByIdAndUserUserIdWithMilestones(roadmapId, otherUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> careerRoadmapService.getRoadmapDetail(roadmapId, otherUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("access denied");
    }

    @Test
    @DisplayName("Should delete roadmap successfully when owned by authenticated user")
    void testDeleteRoadmap_DeletesSuccessfully() {
        UUID roadmapId = UUID.randomUUID();
        CareerRoadmap roadmap = new CareerRoadmap(testProfile, "Staff Engineer", "Senior");
        roadmap.setId(roadmapId);

        when(careerRoadmapRepository.findByIdAndUserUserId(roadmapId, testUserId)).thenReturn(Optional.of(roadmap));
        doNothing().when(careerRoadmapRepository).delete(roadmap);

        careerRoadmapService.deleteRoadmap(roadmapId, testUserId);

        verify(careerRoadmapRepository, times(1)).delete(roadmap);
    }
}
