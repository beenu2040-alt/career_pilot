package com.careerpilot.service;

import com.careerpilot.dto.ProfileResponse;
import com.careerpilot.dto.ProfileUpdateRequest;
import com.careerpilot.entity.Profile;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(profileRepository);
    }

    @Test
    @DisplayName("getProfile returns profile response when profile exists")
    void shouldReturnProfileWhenFound() {
        UUID userId = UUID.randomUUID();
        Profile profile = new Profile(userId, "Jane Doe", "Software Engineer", "New York", "Tech Lead");

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        ProfileResponse result = profileService.getProfile(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.fullName()).isEqualTo("Jane Doe");
        assertThat(result.headline()).isEqualTo("Software Engineer");
    }

    @Test
    @DisplayName("getProfile throws ResourceNotFoundException when profile does not exist")
    void shouldThrowWhenNotFound() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    @DisplayName("upsertProfile creates new profile if one does not exist")
    void shouldCreateNewProfileWhenNoneExists() {
        UUID userId = UUID.randomUUID();
        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "Jane Doe",
                "Software Engineer",
                "New York",
                "Engineering Manager"
        );

        when(profileRepository.findById(userId)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResponse response = profileService.upsertProfile(userId, request);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.fullName()).isEqualTo("Jane Doe");
        assertThat(response.targetRole()).isEqualTo("Engineering Manager");
        verify(profileRepository, times(1)).save(any(Profile.class));
    }

    @Test
    @DisplayName("upsertProfile updates existing profile fields")
    void shouldUpdateExistingProfile() {
        UUID userId = UUID.randomUUID();
        Profile existing = new Profile(userId, "Jane Doe", "Software Engineer", "New York", "Tech Lead");
        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "Jane Smith",
                "Senior Staff Engineer",
                "San Francisco",
                "VP of Engineering"
        );

        when(profileRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(profileRepository.save(existing)).thenReturn(existing);

        ProfileResponse response = profileService.upsertProfile(userId, request);

        assertThat(response.fullName()).isEqualTo("Jane Smith");
        assertThat(response.headline()).isEqualTo("Senior Staff Engineer");
        assertThat(response.location()).isEqualTo("San Francisco");
        assertThat(response.targetRole()).isEqualTo("VP of Engineering");
    }
}
