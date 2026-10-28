package com.careerpilot.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications", uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_job_application", columnNames = {"user_id", "job_id"})
})
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "job_url", length = 512)
    private String jobUrl;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "salary_offered", precision = 12, scale = 2)
    private BigDecimal salaryOffered;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;

    @Column(name = "interview_at")
    private Instant interviewAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<ApplicationHistory> history = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Application() {
    }

    public Application(Profile profile, Job job, String status, Instant appliedAt, String notes) {
        this.profile = profile;
        this.job = job;
        this.companyName = (job != null && job.getCompany() != null) ? job.getCompany() : "Unknown Employer";
        this.jobTitle = (job != null && job.getTitle() != null) ? job.getTitle() : "Target Position";
        this.jobDescription = (job != null) ? job.getDescription() : null;
        this.jobUrl = (job != null) ? job.getSourceUrl() : null;
        this.status = status;
        this.appliedAt = appliedAt;
        this.notes = notes;
    }

    public Application(
            Profile profile,
            Job job,
            String companyName,
            String jobTitle,
            String jobDescription,
            String jobUrl,
            String contactName,
            String contactEmail,
            String status,
            Instant appliedAt,
            LocalDate nextFollowUpDate,
            Instant interviewAt,
            String notes
    ) {
        this.profile = profile;
        this.job = job;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.jobDescription = jobDescription;
        this.jobUrl = jobUrl;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.status = status;
        this.appliedAt = appliedAt;
        this.nextFollowUpDate = nextFollowUpDate;
        this.interviewAt = interviewAt;
        this.notes = notes;
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

    public void addHistory(ApplicationHistory entry) {
        this.history.add(entry);
        entry.setApplication(this);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public BigDecimal getSalaryOffered() {
        return salaryOffered;
    }

    public void setSalaryOffered(BigDecimal salaryOffered) {
        this.salaryOffered = salaryOffered;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDate getNextFollowUpDate() {
        return nextFollowUpDate;
    }

    public void setNextFollowUpDate(LocalDate nextFollowUpDate) {
        this.nextFollowUpDate = nextFollowUpDate;
    }

    public Instant getInterviewAt() {
        return interviewAt;
    }

    public void setInterviewAt(Instant interviewAt) {
        this.interviewAt = interviewAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ApplicationHistory> getHistory() {
        return history;
    }

    public void setHistory(List<ApplicationHistory> history) {
        this.history = history;
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
