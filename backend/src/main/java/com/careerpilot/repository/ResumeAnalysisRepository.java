package com.careerpilot.repository;

import com.careerpilot.entity.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, UUID> {

    List<ResumeAnalysis> findByResumeIdAndUserUserIdOrderByCreatedAtDesc(UUID resumeId, UUID userId);

    Optional<ResumeAnalysis> findByIdAndUserUserId(UUID id, UUID userId);

    Optional<ResumeAnalysis> findFirstByResumeIdAndUserUserIdOrderByCreatedAtDesc(UUID resumeId, UUID userId);

    Optional<ResumeAnalysis> findFirstByUserUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT ra FROM ResumeAnalysis ra LEFT JOIN FETCH ra.job WHERE ra.id = :id AND ra.user.userId = :userId")
    Optional<ResumeAnalysis> findByIdAndUserUserIdWithJob(@Param("id") UUID id, @Param("userId") UUID userId);
}
