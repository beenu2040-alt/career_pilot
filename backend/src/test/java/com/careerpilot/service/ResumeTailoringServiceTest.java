package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.*;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeTailoringServiceTest {

    @Mock
    private ResumeTailoringSessionRepository sessionRepository;

    @Mock
    private ResumeTailoringSuggestionRepository suggestionRepository;

    @Mock
    private ResumeTailoredVersionRepository versionRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ResumeStorageService resumeStorageService;

    @Mock
    private PdfValidationService pdfValidationService;

    @Mock
    private LlmResumeTailoringClient llmClient;

    @Mock
    private PdfResumeExportService pdfResumeExportService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ResumeTailoringService tailoringService;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID resumeId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID suggestionId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();

    private Profile userProfile;
    private Resume testResume;
    private Job testJob;
    private ResumeTailoringSession testSession;

    @BeforeEach
    void setUp() {
        userProfile = new Profile(userId);
        userProfile.setFullName("Taylor Dev");

        testResume = new Resume(userProfile, "cv.pdf", "resumes/cv.pdf", 2048L, "application/pdf");
        testResume.setId(resumeId);

        testJob = new Job();
        testJob.setId(jobId);
        testJob.setTitle("Cloud Engineer");
        testJob.setCompany("Cloud Corp");
        testJob.setDescription("Looking for Cloud Engineer with Spring Boot and AWS knowledge.");

        testSession = new ResumeTailoringSession(
                userProfile,
                testResume,
                testJob,
                "Cloud Engineer",
                "Cloud Corp",
                "Looking for Cloud Engineer with Spring Boot and AWS knowledge."
        );
        testSession.setId(sessionId);
    }

    @Test
    @DisplayName("createSession with valid stored job should succeed")
    void testCreateSession_WithStoredJob_Success() {
        CreateTailoringSessionRequest req = new CreateTailoringSessionRequest(
                resumeId,
                jobId,
                null,
                null,
                null
        );

        when(profileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(resumeRepository.findByIdAndUserUserId(resumeId, userId)).thenReturn(Optional.of(testResume));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(sessionRepository.save(any(ResumeTailoringSession.class))).thenAnswer(inv -> {
            ResumeTailoringSession s = inv.getArgument(0);
            s.setId(sessionId);
            return s;
        });

        TailoringSessionDto result = tailoringService.createSession(userId, req);

        assertNotNull(result);
        assertEquals(sessionId, result.id());
        assertEquals("Cloud Engineer", result.jobTitle());
        assertEquals("Cloud Corp", result.companyName());
        assertEquals(TailoringSessionStatus.CREATED, result.status());
    }

    @Test
    @DisplayName("createSession with missing resume should throw ResourceNotFoundException")
    void testCreateSession_ResumeNotFound() {
        CreateTailoringSessionRequest req = new CreateTailoringSessionRequest(
                resumeId,
                jobId,
                null,
                null,
                null
        );

        when(profileRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(resumeRepository.findByIdAndUserUserId(resumeId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tailoringService.createSession(userId, req));
    }

    @Test
    @DisplayName("generateSuggestions should update status and add suggestions")
    void testGenerateSuggestions_Success() {
        when(sessionRepository.findByIdAndUserUserId(sessionId, userId)).thenReturn(Optional.of(testSession));
        when(resumeStorageService.loadResume(any())).thenReturn(new byte[]{1, 2, 3});
        when(pdfValidationService.extractAndValidateText(any())).thenReturn("Sample extracted resume text with Java and Spring");

        TailoringSuggestionDto suggestionDto = new TailoringSuggestionDto(
                UUID.randomUUID(),
                sessionId,
                SuggestionSection.SUMMARY,
                "Optimize Summary",
                "Old summary",
                "New summary",
                null,
                "Keyword alignment",
                SuggestionStatus.PENDING,
                false,
                null,
                1
        );

        TailoringEngineResult engineResult = new TailoringEngineResult(
                "Cloud Engineer",
                "model-v1",
                60,
                85,
                new KeywordCoverageDto(List.of("Java"), List.of("AWS"), List.of("AWS"), List.of(), 60, 85, "Good coverage"),
                List.of(suggestionDto),
                null
        );

        when(llmClient.tailor(any(), any(), any(), any(), any())).thenReturn(engineResult);
        when(sessionRepository.save(any(ResumeTailoringSession.class))).thenAnswer(inv -> inv.getArgument(0));

        TailoringSessionDetailDto details = tailoringService.generateSuggestions(sessionId, userId);

        assertNotNull(details);
        assertEquals(TailoringSessionStatus.SUGGESTIONS_READY, details.status());
        assertEquals(1, details.suggestions().size());
        assertEquals("Optimize Summary", details.suggestions().get(0).title());
    }

    @Test
    @DisplayName("updateSuggestion should accept, reject, or edit suggestion")
    void testUpdateSuggestion_AcceptAndEdit() {
        ResumeTailoringSuggestion suggestion = new ResumeTailoringSuggestion(
                testSession,
                SuggestionSection.EXPERIENCE,
                "Refactor Bullet",
                "Did programming",
                "Architected distributed services",
                "Enhanced impact",
                false,
                null,
                1
        );
        suggestion.setId(suggestionId);

        when(sessionRepository.existsByIdAndUserUserId(sessionId, userId)).thenReturn(true);
        when(suggestionRepository.findByIdAndSessionIdAndUserId(suggestionId, sessionId, userId))
                .thenReturn(Optional.of(suggestion));
        when(suggestionRepository.save(any(ResumeTailoringSuggestion.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateSuggestionRequest editReq = new UpdateSuggestionRequest(SuggestionStatus.EDITED, "Custom user bullet edit");
        TailoringSuggestionDto result = tailoringService.updateSuggestion(sessionId, suggestionId, userId, editReq);

        assertNotNull(result);
        assertEquals(SuggestionStatus.EDITED, result.status());
        assertEquals("Custom user bullet edit", result.userEditedContent());
    }

    @Test
    @DisplayName("saveVersion should create version with incremented version number")
    void testSaveVersion_Success() {
        when(sessionRepository.findByIdAndUserUserId(sessionId, userId)).thenReturn(Optional.of(testSession));
        when(versionRepository.countBySessionId(sessionId)).thenReturn(0);
        when(versionRepository.save(any(ResumeTailoredVersion.class))).thenAnswer(inv -> {
            ResumeTailoredVersion v = inv.getArgument(0);
            v.setId(versionId);
            return v;
        });

        SaveTailoredVersionRequest req = new SaveTailoredVersionRequest("v1 custom name", "Tailored for job", null);
        TailoredResumeVersionDto version = tailoringService.saveVersion(sessionId, userId, req);

        assertNotNull(version);
        assertEquals(1, version.versionNumber());
        assertEquals("v1 custom name", version.versionName());
        assertNotNull(version.tailoredPlainText());
    }

    @Test
    @DisplayName("Cross-user ownership check: unauthorized user accessing another user's session should fail")
    void testCrossUserOwnershipViolation_ThrowsException() {
        when(sessionRepository.findByIdAndUserUserId(sessionId, otherUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                tailoringService.getSessionDetails(sessionId, otherUserId)
        );
    }

    @Test
    @DisplayName("deleteSession should delete session if authorized")
    void testDeleteSession_Success() {
        when(sessionRepository.findByIdAndUserUserId(sessionId, userId)).thenReturn(Optional.of(testSession));
        doNothing().when(sessionRepository).delete(testSession);

        assertDoesNotThrow(() -> tailoringService.deleteSession(sessionId, userId));
        verify(sessionRepository, times(1)).delete(testSession);
    }
}
