package com.careerpilot.repository;

import com.careerpilot.entity.ApplicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationHistoryRepository extends JpaRepository<ApplicationHistory, UUID> {

    List<ApplicationHistory> findByApplicationIdAndUserUserIdOrderByCreatedAtDesc(UUID applicationId, UUID userId);

    List<ApplicationHistory> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);

    List<ApplicationHistory> findByUserUserId(UUID userId);
}
