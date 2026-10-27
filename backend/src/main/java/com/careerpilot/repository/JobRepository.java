package com.careerpilot.repository;

import com.careerpilot.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

    @EntityGraph(attributePaths = {"jobSkills", "jobSkills.skill"})
    Optional<Job> findWithSkillsById(UUID id);

    @EntityGraph(attributePaths = {"jobSkills", "jobSkills.skill"})
    @Query("SELECT j FROM Job j WHERE j.id = :id AND j.isActive = true")
    Optional<Job> findActiveWithSkillsById(UUID id);

    @EntityGraph(attributePaths = {"jobSkills", "jobSkills.skill"})
    @Query("SELECT j FROM Job j WHERE j.isActive = true")
    List<Job> findAllActiveWithSkills();

    @EntityGraph(attributePaths = {"jobSkills", "jobSkills.skill"})
    Page<Job> findAll(Specification<Job> spec, Pageable pageable);

    long countByIsActiveTrue();
}
