package com.careerpilot.repository;

import com.careerpilot.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    List<Application> findByProfileUserId(UUID userId);
    Optional<Application> findByProfileUserIdAndJobId(UUID userId, UUID jobId);
}
