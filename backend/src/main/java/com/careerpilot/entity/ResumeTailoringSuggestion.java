package com.careerpilot.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_tailoring_suggestions")
public class ResumeTailoringSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ResumeTailoringSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "section", nullable = false, length = 50)
    private SuggestionSection section;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "suggested_content", nullable = false, columnDefinition = "TEXT")
    private String suggestedContent;

    @Column(name = "user_edited_content", columnDefinition = "TEXT")
    private String userEditedContent;

    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @Column(name = "requires_user_confirmation", nullable = false)
    private boolean requiresUserConfirmation = false;

    @Column(name = "confirmation_prompt", columnDefinition = "TEXT")
    private String confirmationPrompt;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ResumeTailoringSuggestion() {
    }

    public ResumeTailoringSuggestion(
            ResumeTailoringSession session,
            SuggestionSection section,
            String title,
            String originalContent,
            String suggestedContent,
            String explanation,
            boolean requiresUserConfirmation,
            String confirmationPrompt,
            int sequenceOrder
    ) {
        this.session = session;
        this.section = section;
        this.title = title;
        this.originalContent = originalContent;
        this.suggestedContent = suggestedContent;
        this.explanation = explanation;
        this.requiresUserConfirmation = requiresUserConfirmation;
        this.confirmationPrompt = confirmationPrompt;
        this.sequenceOrder = sequenceOrder;
        this.status = SuggestionStatus.PENDING;
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

    public SuggestionSection getSection() {
        return section;
    }

    public void setSection(SuggestionSection section) {
        this.section = section;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getSuggestedContent() {
        return suggestedContent;
    }

    public void setSuggestedContent(String suggestedContent) {
        this.suggestedContent = suggestedContent;
    }

    public String getUserEditedContent() {
        return userEditedContent;
    }

    public void setUserEditedContent(String userEditedContent) {
        this.userEditedContent = userEditedContent;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }

    public boolean isRequiresUserConfirmation() {
        return requiresUserConfirmation;
    }

    public void setRequiresUserConfirmation(boolean requiresUserConfirmation) {
        this.requiresUserConfirmation = requiresUserConfirmation;
    }

    public String getConfirmationPrompt() {
        return confirmationPrompt;
    }

    public void setConfirmationPrompt(String confirmationPrompt) {
        this.confirmationPrompt = confirmationPrompt;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
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
