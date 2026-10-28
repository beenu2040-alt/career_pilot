package com.careerpilot.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "resume_tailoring_sessions")
public class ResumeTailoringSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Profile user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "job_description_snapshot", nullable = false, columnDefinition = "TEXT")
    private String jobDescriptionSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TailoringSessionStatus status = TailoringSessionStatus.CREATED;

    @Column(name = "target_role", length = 150)
    private String targetRole;

    @Column(name = "model_identifier", length = 100)
    private String modelIdentifier;

    @Column(name = "match_score_before")
    private Integer matchScoreBefore;

    @Column(name = "match_score_after")
    private Integer matchScoreAfter;

    @Column(name = "keyword_coverage", columnDefinition = "TEXT")
    private String keywordCoverage;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    private List<ResumeTailoringSuggestion> suggestions = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("versionNumber DESC")
    private List<ResumeTailoredVersion> versions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ResumeTailoringSession() {
    }

    public ResumeTailoringSession(
            Profile user,
            Resume resume,
            Job job,
            String jobTitle,
            String companyName,
            String jobDescriptionSnapshot
    ) {
        this.user = user;
        this.resume = resume;
        this.job = job;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.jobDescriptionSnapshot = jobDescriptionSnapshot;
        this.status = TailoringSessionStatus.CREATED;
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

    public void addSuggestion(ResumeTailoringSuggestion suggestion) {
        suggestions.add(suggestion);
        suggestion.setSession(this);
    }

    public void addVersion(ResumeTailoredVersion version) {
        versions.add(version);
        version.setSession(this);
    }

    // Getters and Setters
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

    public Resume getResume() {
        return resume;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobDescriptionSnapshot() {
        return jobDescriptionSnapshot;
    }

    public void setJobDescriptionSnapshot(String jobDescriptionSnapshot) {
        this.jobDescriptionSnapshot = jobDescriptionSnapshot;
    }

    public TailoringSessionStatus getStatus() {
        return status;
    }

    public void setStatus(TailoringSessionStatus status) {
        this.status = status;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getModelIdentifier() {
        return modelIdentifier;
    }

    public void setModelIdentifier(String modelIdentifier) {
        this.modelIdentifier = modelIdentifier;
    }

    public Integer getMatchScoreBefore() {
        return matchScoreBefore;
    }

    public void setMatchScoreBefore(Integer matchScoreBefore) {
        this.matchScoreBefore = matchScoreBefore;
    }

    public Integer getMatchScoreAfter() {
        return matchScoreAfter;
    }

    public void setMatchScoreAfter(Integer matchScoreAfter) {
        this.matchScoreAfter = matchScoreAfter;
    }

    public String getKeywordCoverage() {
        return keywordCoverage;
    }

    public void setKeywordCoverage(String keywordCoverage) {
        this.keywordCoverage = keywordCoverage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<ResumeTailoringSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<ResumeTailoringSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public List<ResumeTailoredVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<ResumeTailoredVersion> versions) {
        this.versions = versions;
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
