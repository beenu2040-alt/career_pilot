package com.careerpilot.service;

import com.careerpilot.dto.SkillDto;
import com.careerpilot.dto.UserSkillCreateRequest;
import com.careerpilot.dto.UserSkillDto;
import com.careerpilot.entity.Profile;
import com.careerpilot.entity.Skill;
import com.careerpilot.entity.UserSkill;
import com.careerpilot.entity.UserSkillId;
import com.careerpilot.repository.ProfileRepository;
import com.careerpilot.repository.SkillRepository;
import com.careerpilot.repository.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProfileRepository profileRepository;
    private final SkillNormalizer skillNormalizer;

    public SkillService(
            SkillRepository skillRepository,
            UserSkillRepository userSkillRepository,
            ProfileRepository profileRepository,
            SkillNormalizer skillNormalizer
    ) {
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.profileRepository = profileRepository;
        this.skillNormalizer = skillNormalizer;
    }

    public List<SkillDto> getAllSkills() {
        return skillRepository.findAllByOrderByNameAsc().stream()
                .map(s -> new SkillDto(s.getId(), s.getName(), s.getCategory(), false))
                .toList();
    }

    public List<UserSkillDto> getUserSkills(UUID userId) {
        return userSkillRepository.findByProfileUserId(userId).stream()
                .map(us -> new UserSkillDto(
                        us.getSkill().getId(),
                        us.getSkill().getName(),
                        us.getSkill().getCategory(),
                        us.getProficiencyLevel(),
                        us.getYearsExperience()
                ))
                .sorted(Comparator.comparing(UserSkillDto::skillName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public UserSkillDto addUserSkill(UUID userId, UserSkillCreateRequest request) {
        Profile profile = profileRepository.findById(userId)
                .orElseGet(() -> profileRepository.save(new Profile(userId)));

        String rawName = request.skillName().trim();
        Skill skill = skillRepository.findByNameIgnoreCase(rawName)
                .orElseGet(() -> {
                    String category = (request.category() != null && !request.category().isBlank())
                            ? request.category().trim()
                            : "General";
                    return skillRepository.save(new Skill(rawName, category));
                });

        UserSkill userSkill = new UserSkill(
                profile,
                skill,
                request.proficiencyLevel(),
                request.yearsExperience() != null ? request.yearsExperience() : 0
        );

        userSkillRepository.save(userSkill);

        return new UserSkillDto(
                skill.getId(),
                skill.getName(),
                skill.getCategory(),
                userSkill.getProficiencyLevel(),
                userSkill.getYearsExperience()
        );
    }

    @Transactional
    public void removeUserSkill(UUID userId, UUID skillId) {
        UserSkillId id = new UserSkillId(userId, skillId);
        if (userSkillRepository.existsById(id)) {
            userSkillRepository.deleteById(id);
        }
    }
}
