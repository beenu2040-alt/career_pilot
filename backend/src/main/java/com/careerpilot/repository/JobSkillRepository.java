package com.careerpilot.repository;

import com.careerpilot.entity.JobSkill;
import com.careerpilot.entity.JobSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {
    List<JobSkill> findByJobId(UUID jobId);
    void deleteByJobId(UUID jobId);
}
