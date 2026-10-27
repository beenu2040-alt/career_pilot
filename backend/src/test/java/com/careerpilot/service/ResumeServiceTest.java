package com.careerpilot.service;

import com.careerpilot.dto.*;
import com.careerpilot.entity.*;
import com.careerpilot.exception.ResourceNotFoundException;
import com.careerpilot.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private ResumeAnalysisRepository resumeAnalysisRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private PdfValidationService pdfValidationService;
    @Mock
    private ResumeStorageService resumeStorageService;
    @Mock
    private LlmResumeAnalysisClient analysisClient;

    private ResumeService resumeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private UUID resumeId;
    private Profile profile;

    @BeforeEach
    void setUp() {
        resumeService = new ResumeService(
                resumeRepository,
                resumeAnalysisRepository,
                profileRepository,
                jobRepository,
                pdfValidationService,
                resumeStorageService,
                analysisClient,
                objectMapper
        );

        userId = UUID.randomUUID();
        resumeId = UUID.randomUUID();
        profile = new Profile(userId);
    }

    @Test
    void testUploadResume_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "my-resume.pdf", "application/pdf", "%PDF-sample".getBytes()
        );

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(resumeStorageService.storeResume(eq(userId), any(UUID.class), eq("my-resume.pdf"), any(byte[].class)))
                .thenReturn("local://" + userId + "/" + resumeId + ".pdf");

        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(resumeId);
            return r;
        });

        ResumeSummaryDto result = resumeService.uploadResume(userId, file);

        assertNotNull(result);
        assertEquals("my-resume.pdf", result.originalFilename());
        verify(pdfValidationService).validateUploadFile(file);
        verify(resumeStorageService).storeResume(eq(userId), any(UUID.class), eq("my-resume.pdf"), any(byte[].class));
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    void testGetUserResumes_Success() {
        Resume resume = new Resume(profile, "cv.pdf", "local://path", 1024L, "application/pdf");
        resume.setId(resumeId);

        when(resumeRepository.findByUserUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(resume));
        when(resumeAnalysisRepository.findFirstByResumeIdAndUserUserIdOrderByCreatedAtDesc(resumeId, userId))
                .thenReturn(Optional.empty());

        List<ResumeSummaryDto> list = resumeService.getUserResumes(userId);

        assertEquals(1, list.size());
        assertEquals(resumeId, list.get(0).id());
    }

    @Test
    void testGetResumeById_NotFound() {
        when(resumeRepository.findByIdAndUserUserId(resumeId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resumeService.getResumeById(resumeId, userId));
    }

    @Test
    void testDeleteResume_Success() {
        Resume resume = new Resume(profile, "cv.pdf", "local://path", 1024L, "application/pdf");
        resume.setId(resumeId);

        when(resumeRepository.findByIdAndUserUserId(resumeId, userId)).thenReturn(Optional.of(resume));

        resumeService.deleteResume(resumeId, userId);

        verify(resumeRepository).delete(resume);
        verify(resumeStorageService).deleteResume("local://path");
    }

    @Test
    void testAnalyzeResume_Success() {
        Resume resume = new Resume(profile, "cv.pdf", "local://path", 1024L, "application/pdf");
        resume.setId(resumeId);

        when(resumeRepository.findByIdAndUserUserId(resumeId, userId)).thenReturn(Optional.of(resume));
        when(resumeStorageService.loadResume("local://path")).thenReturn("%PDF-content".getBytes());
        when(pdfValidationService.extractAndValidateText(any(byte[].class))).thenReturn("Extracted text");

        ResumeAnalysisResult mockResult = new ResumeAnalysisResult(
                "Executive Summary",
                82,
                List.of(new DimensionScoreDto("Skills", 22, 25, 25, "HYBRID", "Great")),
                new ExtractedDataDto("Alex", "alex@test.com", "555", "NYC", List.of(), 500, 3, List.of(), List.of()),
                List.of(new StrengthDto("Impact", "Clear numbers", "Increased by 30%")),
                List.of(new ImprovementDto("HIGH", "Bullets", "Active Verbs", "Passive words", "Use strong verbs", "Worked on", "Engineered")),
                new SkillAnalysisDto(List.of(), List.of(), List.of()),
                new ExperienceAnalysisDto(2, "STRONG", "QUANTIFIED", List.of()),
                null,
                "test-model"
        );

        when(analysisClient.analyze(eq("Extracted text"), any())).thenReturn(mockResult);

        when(resumeAnalysisRepository.save(any(ResumeAnalysis.class))).thenAnswer(inv -> {
            ResumeAnalysis ra = inv.getArgument(0);
            ra.setId(UUID.randomUUID());
            return ra;
        });

        ResumeAnalysisDto analysisDto = resumeService.analyzeResume(resumeId, userId, Optional.empty());

        assertNotNull(analysisDto);
        assertEquals(82, analysisDto.overallScore());
        assertEquals("Executive Summary", analysisDto.summary());
        verify(resumeRepository, atLeastOnce()).save(resume);
        verify(resumeAnalysisRepository).save(any(ResumeAnalysis.class));
    }
}
