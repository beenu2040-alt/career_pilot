package com.careerpilot.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "career_roadmaps")
public class CareerRoadmap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Profile user;

    @Column(name = "target_role", nullable = false, length = 150)
    private String targetRole;

    @Column(name = "current_level", length = 50)
    private String currentLevel = "Mid-Level";

    @Column(name = "overall_progress", nullable = false)
    private int overallProgress = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RoadmapStatus status = RoadmapStatus.NOT_STARTED;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "skill_gap_summary", columnDefinition = "TEXT")
    private String skillGapSummary;

    @Column(name = "model_identifier", length = 100)
    private String modelIdentifier;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    private List<RoadmapMilestone> milestones = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CareerRoadmap() {
    }

    public CareerRoadmap(Profile user, String targetRole, String currentLevel) {
        this.user = user;
        this.targetRole = targetRole;
        this.currentLevel = currentLevel;
        this.status = RoadmapStatus.NOT_STARTED;
        this.overallProgress = 0;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addMilestone(RoadmapMilestone milestone) {
        milestones.add(milestone);
        milestone.setRoadmap(this);
    }

    public void recalculateProgress() {
        if (milestones == null || milestones.isEmpty()) {
            this.overallProgress = 0;
            this.status = RoadmapStatus.NOT_STARTED;
            return;
        }

        int total = milestones.size();
        long completed = milestones.stream().filter(m -> m.getStatus() == MilestoneStatus.COMPLETED).count();
        long inProgress = milestones.stream().filter(m -> m.getStatus() == MilestoneStatus.IN_PROGRESS).count();

        this.overallProgress = (int) Math.round(((double) completed / total) * 100.0);

        if (completed == total) {
            this.status = RoadmapStatus.COMPLETED;
        } else if (completed > 0 || inProgress > 0) {
            this.status = RoadmapStatus.IN_PROGRESS;
        } else {
            this.status = RoadmapStatus.NOT_STARTED;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Profile getUser() {
        return user;
    }

    public void setUser(Profile user) {
        this.user = user;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getOverallProgress() {
        return overallProgress;
    }

    public void setOverallProgress(int overallProgress) {
        this.overallProgress = overallProgress;
    }

    public RoadmapStatus getStatus() {
        return status;
    }

    public void setStatus(RoadmapStatus status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSkillGapSummary() {
        return skillGapSummary;
    }

    public void setSkillGapSummary(String skillGapSummary) {
        this.skillGapSummary = skillGapSummary;
    }

    public String getModelIdentifier() {
        return modelIdentifier;
    }

    public void setModelIdentifier(String modelIdentifier) {
        this.modelIdentifier = modelIdentifier;
    }

    public List<RoadmapMilestone> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<RoadmapMilestone> milestones) {
        this.milestones = milestones;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
