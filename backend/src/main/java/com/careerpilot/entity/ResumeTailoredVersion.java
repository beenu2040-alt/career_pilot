package com.careerpilot.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_tailored_versions")
public class ResumeTailoredVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ResumeTailoringSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Profile user;

    @Column(name = "version_number", nullable = false)
    private int versionNumber = 1;

    @Column(name = "version_name", nullable = false)
    private String versionName;

    @Column(name = "tailored_content", nullable = false, columnDefinition = "TEXT")
    private String tailoredContent;

    @Column(name = "tailored_plain_text", nullable = false, columnDefinition = "TEXT")
    private String tailoredPlainText;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "applied_suggestions_count", nullable = false)
    private int appliedSuggestionsCount = 0;

    @Column(name = "keyword_coverage", columnDefinition = "TEXT")
    private String keywordCoverage;

    @Column(name = "storage_path", length = 512)
    private String storagePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ResumeTailoredVersion() {
    }

    public ResumeTailoredVersion(
            ResumeTailoringSession session,
            Profile user,
            int versionNumber,
            String versionName,
            String tailoredContent,
            String tailoredPlainText,
            String changeSummary,
            int appliedSuggestionsCount
    ) {
        this.session = session;
        this.user = user;
        this.versionNumber = versionNumber;
        this.versionName = versionName;
        this.tailoredContent = tailoredContent;
        this.tailoredPlainText = tailoredPlainText;
        this.changeSummary = changeSummary;
        this.appliedSuggestionsCount = appliedSuggestionsCount;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ResumeTailoringSession getSession() {
        return session;
    }

    public void setSession(ResumeTailoringSession session) {
        this.session = session;
    }

    public Profile getUser() {
        return user;
    }

    public void setUser(Profile user) {
        this.user = user;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getTailoredContent() {
        return tailoredContent;
    }

    public void setTailoredContent(String tailoredContent) {
        this.tailoredContent = tailoredContent;
    }

    public String getTailoredPlainText() {
        return tailoredPlainText;
    }

    public void setTailoredPlainText(String tailoredPlainText) {
        this.tailoredPlainText = tailoredPlainText;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public int getAppliedSuggestionsCount() {
        return appliedSuggestionsCount;
    }

    public void setAppliedSuggestionsCount(int appliedSuggestionsCount) {
        this.appliedSuggestionsCount = appliedSuggestionsCount;
    }

    public String getKeywordCoverage() {
        return keywordCoverage;
    }

    public void setKeywordCoverage(String keywordCoverage) {
        this.keywordCoverage = keywordCoverage;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
