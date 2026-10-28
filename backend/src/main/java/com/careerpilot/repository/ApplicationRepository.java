package com.careerpilot.repository;

import com.careerpilot.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByProfileUserId(UUID userId);

    List<Application> findByProfileUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<Application> findByIdAndProfileUserId(UUID id, UUID userId);

    boolean existsByProfileUserIdAndJobId(UUID userId, UUID jobId);

    Optional<Application> findByProfileUserIdAndJobId(UUID userId, UUID jobId);

    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.job j " +
           "WHERE a.id = :id AND a.profile.userId = :userId")
    Optional<Application> findByIdAndProfileUserIdWithJob(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT a FROM Application a WHERE a.profile.userId = :userId AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:query IS NULL OR LOWER(a.companyName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.jobTitle) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Application> searchApplications(
            @Param("userId") UUID userId,
            @Param("status") String status,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("SELECT a FROM Application a WHERE a.profile.userId = :userId AND a.nextFollowUpDate IS NOT NULL AND a.status NOT IN ('REJECTED', 'WITHDRAWN') ORDER BY a.nextFollowUpDate ASC")
    List<Application> findActiveFollowUps(@Param("userId") UUID userId);

    @Query("SELECT a FROM Application a WHERE a.profile.userId = :userId AND a.interviewAt >= :now AND a.status NOT IN ('REJECTED', 'WITHDRAWN') ORDER BY a.interviewAt ASC")
    List<Application> findUpcomingInterviews(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT a FROM Application a WHERE a.profile.userId = :userId AND a.status IN ('APPLIED', 'SCREENING', 'INTERVIEW', 'ASSESSMENT', 'INTERVIEWING') AND a.nextFollowUpDate IS NULL AND a.updatedAt < :staleThreshold ORDER BY a.updatedAt ASC")
    List<Application> findStaleApplications(@Param("userId") UUID userId, @Param("staleThreshold") Instant staleThreshold);

    long countByProfileUserId(UUID userId);
}
