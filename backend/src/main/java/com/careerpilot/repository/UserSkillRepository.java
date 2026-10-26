package com.careerpilot.repository;

import com.careerpilot.entity.UserSkill;
import com.careerpilot.entity.UserSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, UserSkillId> {
    List<UserSkill> findByProfileUserId(UUID userId);
}
