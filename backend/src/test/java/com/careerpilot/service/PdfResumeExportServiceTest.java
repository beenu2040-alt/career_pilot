package com.careerpilot.service;

import com.careerpilot.dto.TailoredExperienceDto;
import com.careerpilot.dto.TailoredProjectDto;
import com.careerpilot.dto.TailoredResumeContentDto;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfResumeExportServiceTest {

    private final PdfResumeExportService pdfExportService = new PdfResumeExportService();

    @Test
    @DisplayName("generateResumePdf should produce valid non-empty PDF bytes containing resume content")
    void testGenerateResumePdf_Success() throws IOException {
        TailoredResumeContentDto content = new TailoredResumeContentDto(
                "Alex Rivera",
                "alex.rivera@example.com",
                "+1 (555) 234-5678",
                "Austin, TX",
                "Dedicated Full-Stack Software Engineer with 4 years of experience building high-scale distributed applications.",
                List.of("Java", "Spring Boot", "TypeScript", "React", "PostgreSQL", "Docker", "AWS"),
                List.of(
                        new TailoredExperienceDto(
                                "Senior Software Engineer",
                                "CloudScale Tech",
                                "2022 - Present",
                                List.of(
                                        "Architected and deployed resilient microservices handling 12,000 requests per second.",
                                        "Optimized SQL query performance across 50M+ rows, reducing latency by 45%."
                                )
                        )
                ),
                List.of(
                        new TailoredProjectDto(
                                "CareerPilot AI Engine",
                                "Automated career development workflow engine with LLM integrations.",
                                List.of("Java", "Spring Boot", "React"),
                                List.of("Built end-to-end resume tailoring and PDF generation pipeline.")
                        )
                ),
                List.of("B.S. in Computer Science - University of Texas at Austin"),
                List.of("ATS compliant formatting")
        );

        byte[] pdfBytes = pdfExportService.generateResumePdf(content, "Staff Backend Engineer");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF bytes should be non-trivial");

        // Validate PDF magic bytes "%PDF-"
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
        assertEquals('-', (char) pdfBytes[4]);

        // Load into PDFBox to verify validity and extract text
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
            assertFalse(document.isEncrypted());
            assertTrue(document.getNumberOfPages() >= 1);

            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);

            assertTrue(extractedText.contains("Alex Rivera"));
            assertTrue(extractedText.contains("Staff Backend Engineer"));
            assertTrue(extractedText.contains("CloudScale Tech"));
            assertTrue(extractedText.contains("Spring Boot"));
            assertTrue(extractedText.contains("PROFESSIONAL SUMMARY"));
        }
    }
}
