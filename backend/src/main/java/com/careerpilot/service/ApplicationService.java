package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.Application;
import com.careerpilot.entity.ApplicationHistory;
import com.careerpilot.entity.ApplicationStatus;
import com.careerpilot.entity.Job;
import com.careerpilot.entity.Profile;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.ApplicationHistoryRepository;
import com.careerpilot.repository.ApplicationRepository;
import com.careerpilot.repository.JobRepository;
import com.careerpilot.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final ApplicationHistoryRepository historyRepository;
    private final JobRepository jobRepository;
    private final ProfileRepository profileRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            ApplicationHistoryRepository historyRepository,
            JobRepository jobRepository,
            ProfileRepository profileRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public ApplicationDto createApplication(UUID userId, CreateApplicationRequest request) {
        Profile profile = profileRepository.findById(userId)
                .orElseGet(() -> profileRepository.save(new Profile(userId)));

        Job job = null;
        String companyName = request.companyName();
        String jobTitle = request.jobTitle();
        String jobDescription = request.jobDescription();
        String jobUrl = request.jobUrl();

        if (request.jobId() != null) {
            job = jobRepository.findById(request.jobId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + request.jobId()));

            // Prevent duplicate applications for same stored job
            if (applicationRepository.existsByProfileUserIdAndJobId(userId, request.jobId())) {
                throw new IllegalStateException("You already have a tracked application for this job posting.");
            }

            if (companyName == null || companyName.isBlank()) {
                companyName = job.getCompany();
            }
            if (jobTitle == null || jobTitle.isBlank()) {
                jobTitle = job.getTitle();
            }
            if (jobDescription == null || jobDescription.isBlank()) {
                jobDescription = job.getDescription();
            }
            if (jobUrl == null || jobUrl.isBlank()) {
                jobUrl = job.getSourceUrl();
            }
        }

        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name is required.");
        }
        if (jobTitle == null || jobTitle.isBlank()) {
            throw new IllegalArgumentException("Job title is required.");
        }

        ApplicationStatus status = request.status() != null ? request.status() : ApplicationStatus.WISHLIST;
        Instant appliedAt = request.appliedAt();
        if (status != ApplicationStatus.WISHLIST && appliedAt == null) {
            appliedAt = Instant.now();
        }

        Application app = new Application(
                profile,
                job,
                companyName.trim(),
                jobTitle.trim(),
                jobDescription,
                jobUrl,
                request.contactName(),
                request.contactEmail(),
                status.name(),
                appliedAt,
                request.nextFollowUpDate(),
                request.interviewAt(),
                request.notes()
        );
        app.setSalaryOffered(request.salaryOffered());

        Application saved = applicationRepository.save(app);

        // Record initial history entry
        ApplicationHistory history = new ApplicationHistory(
                saved,
                profile,
                null,
                status.name(),
                (request.notes() != null && !request.notes().isBlank()) ? request.notes() : "Application tracked"
        );
        historyRepository.save(history);
        saved.addHistory(history);

        log.info("Created job application: id={}, user={}, company={}, title={}, status={}",
                saved.getId(), userId, companyName, jobTitle, status);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationDto> getApplications(
            UUID userId,
            ApplicationStatus status,
            String query,
            Pageable pageable
    ) {
        String statusStr = status != null ? status.name() : null;
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;

        Page<Application> page = applicationRepository.searchApplications(userId, statusStr, cleanQuery, pageable);
        List<ApplicationDto> dtos = page.getContent().stream().map(this::toDto).toList();

        return new PageResponse<>(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getAllUserApplications(UUID userId) {
        List<Application> apps = applicationRepository.findByProfileUserIdOrderByUpdatedAtDesc(userId);
        return apps.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationDto getApplicationById(UUID id, UUID userId) {
        Application app = applicationRepository.findByIdAndProfileUserIdWithJob(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        return toDto(app);
    }

    @Transactional
    public ApplicationDto updateApplication(UUID id, UUID userId, UpdateApplicationRequest request) {
        Application app = applicationRepository.findByIdAndProfileUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        String oldStatus = app.getStatus();
        String newStatus = request.status() != null ? request.status().name() : oldStatus;

        if (request.companyName() != null && !request.companyName().isBlank()) {
            app.setCompanyName(request.companyName().trim());
        }
        if (request.jobTitle() != null && !request.jobTitle().isBlank()) {
            app.setJobTitle(request.jobTitle().trim());
        }
        app.setJobDescription(request.jobDescription());
        app.setJobUrl(request.jobUrl());
        app.setContactName(request.contactName());
        app.setContactEmail(request.contactEmail());
        app.setSalaryOffered(request.salaryOffered());
        app.setNextFollowUpDate(request.nextFollowUpDate());
        app.setInterviewAt(request.interviewAt());
        app.setNotes(request.notes());

        if (request.appliedAt() != null) {
            app.setAppliedAt(request.appliedAt());
        } else if (!"WISHLIST".equalsIgnoreCase(newStatus) && app.getAppliedAt() == null) {
            app.setAppliedAt(Instant.now());
        }

        if (!oldStatus.equalsIgnoreCase(newStatus)) {
            app.setStatus(newStatus);
            ApplicationHistory history = new ApplicationHistory(
                    app,
                    app.getProfile(),
                    oldStatus,
                    newStatus,
                    "Status updated in application details"
            );
            historyRepository.save(history);
            app.addHistory(history);
        }

        Application saved = applicationRepository.save(app);
        log.info("Updated application: id={}, user={}", id, userId);

        return toDto(saved);
    }

    @Transactional
    public ApplicationDto updateStatus(UUID id, UUID userId, UpdateApplicationStatusRequest request) {
        Application app = applicationRepository.findByIdAndProfileUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        String oldStatus = app.getStatus();
        String newStatus = request.status().name();

        if (!oldStatus.equalsIgnoreCase(newStatus)) {
            app.setStatus(newStatus);
            if (!"WISHLIST".equalsIgnoreCase(newStatus) && app.getAppliedAt() == null) {
                app.setAppliedAt(Instant.now());
            }

            ApplicationHistory history = new ApplicationHistory(
                    app,
                    app.getProfile(),
                    oldStatus,
                    newStatus,
                    request.notes()
            );
            historyRepository.save(history);
            app.addHistory(history);
        }

        Application saved = applicationRepository.save(app);
        log.info("Transitioned application status: id={}, from={}, to={}", id, oldStatus, newStatus);

        return toDto(saved);
    }

    @Transactional
    public void deleteApplication(UUID id, UUID userId) {
        Application app = applicationRepository.findByIdAndProfileUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        applicationRepository.delete(app);
        log.info("Deleted application: id={}, user={}", id, userId);
    }

    @Transactional(readOnly = true)
    public RemindersSummaryDto getReminders(UUID userId) {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        Instant staleThreshold = now.minus(14, ChronoUnit.DAYS);

        List<Application> activeFollowUps = applicationRepository.findActiveFollowUps(userId);
        List<Application> upcomingInterviews = applicationRepository.findUpcomingInterviews(userId, now);
        List<Application> staleApps = applicationRepository.findStaleApplications(userId, staleThreshold);

        List<ApplicationDto> overdue = new ArrayList<>();
        List<ApplicationDto> dueToday = new ArrayList<>();

        for (Application a : activeFollowUps) {
            LocalDate fDate = a.getNextFollowUpDate();
            if (fDate != null) {
                if (fDate.isBefore(today)) {
                    overdue.add(toDto(a));
                } else if (fDate.isEqual(today)) {
                    dueToday.add(toDto(a));
                }
            }
        }

        List<ApplicationDto> interviewDtos = upcomingInterviews.stream().map(this::toDto).toList();
        List<ApplicationDto> staleDtos = staleApps.stream().map(this::toDto).toList();

        return new RemindersSummaryDto(overdue, dueToday, interviewDtos, staleDtos);
    }

    public ApplicationDto toDto(Application a) {
        List<ApplicationHistoryDto> historyDtos = (a.getHistory() != null)
                ? a.getHistory().stream().map(this::toHistoryDto).toList()
                : List.of();

        return new ApplicationDto(
                a.getId(),
                a.getJob() != null ? a.getJob().getId() : null,
                a.getCompanyName(),
                a.getJobTitle(),
                a.getJobDescription(),
                a.getJobUrl(),
                a.getContactName(),
                a.getContactEmail(),
                a.getSalaryOffered(),
                ApplicationStatus.fromString(a.getStatus()),
                a.getAppliedAt(),
                a.getNextFollowUpDate(),
                a.getInterviewAt(),
                a.getNotes(),
                historyDtos,
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private ApplicationHistoryDto toHistoryDto(ApplicationHistory h) {
        return new ApplicationHistoryDto(
                h.getId(),
                h.getApplication().getId(),
                h.getPreviousStatus(),
                h.getNewStatus(),
                h.getNotes(),
                h.getCreatedAt()
        );
    }
}
