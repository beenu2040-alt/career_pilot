package com.careerpilot.service;

import com.careerpilot.dto.ProfileResponse;
import com.careerpilot.dto.ProfileUpdateRequest;
import com.careerpilot.entity.Profile;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));
        return ProfileResponse.fromEntity(profile);
    }

    public ProfileResponse upsertProfile(UUID userId, ProfileUpdateRequest request) {
        Profile profile = profileRepository.findById(userId)
                .orElseGet(() -> new Profile(userId, null, null, null, null));

        if (request.fullName() != null) {
            profile.setFullName(request.fullName().trim());
        }
        if (request.headline() != null) {
            profile.setHeadline(request.headline().trim());
        }
        if (request.location() != null) {
            profile.setLocation(request.location().trim());
        }
        if (request.targetRole() != null) {
            profile.setTargetRole(request.targetRole().trim());
        }

        Profile saved = profileRepository.save(profile);
        return ProfileResponse.fromEntity(saved);
    }
}
