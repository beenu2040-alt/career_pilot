package com.careerpilot.repository;

import com.careerpilot.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Resume> findByIdAndUserUserId(UUID id, UUID userId);

    boolean existsByIdAndUserUserId(UUID id, UUID userId);
}
