package com.careerpilot.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "saved_jobs")
public class SavedJob {

    @EmbeddedId
    private SavedJobId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private Profile user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", insertable = false, updatable = false)
    private Job job;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SavedJob() {
    }

    public SavedJob(Profile user, Job job) {
        this.user = user;
        this.job = job;
        this.id = new SavedJobId(user.getUserId(), job.getId());
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public SavedJobId getId() {
        return id;
    }

    public void setId(SavedJobId id) {
        this.id = id;
    }

    public Profile getUser() {
        return user;
    }

    public void setUser(Profile user) {
        this.user = user;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
