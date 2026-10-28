package com.careerpilot.repository;

import com.careerpilot.entity.ResumeTailoringSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeTailoringSuggestionRepository extends JpaRepository<ResumeTailoringSuggestion, UUID> {

    List<ResumeTailoringSuggestion> findBySessionIdOrderBySequenceOrderAsc(UUID sessionId);

    @Query("SELECT s FROM ResumeTailoringSuggestion s WHERE s.id = :id AND s.session.user.userId = :userId")
    Optional<ResumeTailoringSuggestion> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT s FROM ResumeTailoringSuggestion s WHERE s.id = :id AND s.session.id = :sessionId AND s.session.user.userId = :userId")
    Optional<ResumeTailoringSuggestion> findByIdAndSessionIdAndUserId(
            @Param("id") UUID id,
            @Param("sessionId") UUID sessionId,
            @Param("userId") UUID userId
    );
}
