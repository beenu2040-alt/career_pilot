package com.careerpilot.repository;

import com.careerpilot.entity.CareerRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CareerRoadmapRepository extends JpaRepository<CareerRoadmap, UUID> {

    List<CareerRoadmap> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<CareerRoadmap> findByIdAndUserUserId(UUID id, UUID userId);

    Optional<CareerRoadmap> findFirstByUserUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT cr FROM CareerRoadmap cr LEFT JOIN FETCH cr.milestones WHERE cr.id = :id AND cr.user.userId = :userId")
    Optional<CareerRoadmap> findByIdAndUserUserIdWithMilestones(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT DISTINCT cr FROM CareerRoadmap cr LEFT JOIN FETCH cr.milestones WHERE cr.user.userId = :userId ORDER BY cr.createdAt DESC")
    List<CareerRoadmap> findByUserUserIdWithMilestonesOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
