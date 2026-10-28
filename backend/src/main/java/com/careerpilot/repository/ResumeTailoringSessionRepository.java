package com.careerpilot.repository;

import com.careerpilot.entity.ResumeTailoringSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeTailoringSessionRepository extends JpaRepository<ResumeTailoringSession, UUID> {

    List<ResumeTailoringSession> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ResumeTailoringSession> findByIdAndUserUserId(UUID id, UUID userId);

    boolean existsByIdAndUserUserId(UUID id, UUID userId);

    @Query("SELECT s FROM ResumeTailoringSession s " +
           "LEFT JOIN FETCH s.resume r " +
           "LEFT JOIN FETCH s.job j " +
           "WHERE s.id = :id AND s.user.userId = :userId")
    Optional<ResumeTailoringSession> findByIdAndUserUserIdWithDetails(@Param("id") UUID id, @Param("userId") UUID userId);
}
