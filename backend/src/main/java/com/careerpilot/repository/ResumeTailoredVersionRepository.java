package com.careerpilot.repository;

import com.careerpilot.entity.ResumeTailoredVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeTailoredVersionRepository extends JpaRepository<ResumeTailoredVersion, UUID> {

    List<ResumeTailoredVersion> findBySessionIdOrderByVersionNumberDesc(UUID sessionId);

    List<ResumeTailoredVersion> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ResumeTailoredVersion> findByIdAndUserUserId(UUID id, UUID userId);

    @Query("SELECT v FROM ResumeTailoredVersion v WHERE v.id = :versionId AND v.session.id = :sessionId AND v.user.userId = :userId")
    Optional<ResumeTailoredVersion> findByIdAndSessionIdAndUserId(
            @Param("versionId") UUID versionId,
            @Param("sessionId") UUID sessionId,
            @Param("userId") UUID userId
    );

    int countBySessionId(UUID sessionId);
}
