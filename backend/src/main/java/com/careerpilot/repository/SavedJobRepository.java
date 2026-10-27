package com.careerpilot.repository;

import com.careerpilot.entity.SavedJob;
import com.careerpilot.entity.SavedJobId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, SavedJobId> {

    @EntityGraph(attributePaths = {"job", "job.jobSkills", "job.jobSkills.skill"})
    List<SavedJob> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"job", "job.jobSkills", "job.jobSkills.skill"})
    Page<SavedJob> findByUserUserId(UUID userId, Pageable pageable);

    boolean existsByUserUserIdAndJobId(UUID userId, UUID jobId);

    void deleteByUserUserIdAndJobId(UUID userId, UUID jobId);

    long countByUserUserId(UUID userId);

    @Query("SELECT s.id.jobId FROM SavedJob s WHERE s.id.userId = :userId")
    Set<UUID> findSavedJobIdsByUserId(UUID userId);
}
