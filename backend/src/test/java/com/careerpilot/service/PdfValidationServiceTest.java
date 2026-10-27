package com.careerpilot.service;

import com.careerpilot.exception.InvalidResumeException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PdfValidationServiceTest {

    private PdfValidationService service;

    @BeforeEach
    void setUp() {
        service = new PdfValidationService();
        ReflectionTestUtils.setField(service, "maxFileSize", 10485760L); // 10MB
    }

    private byte[] createSamplePdfBytes(String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(content);
                stream.endText();
            }
            document.save(baos);
        }
        return baos.toByteArray();
    }

    @Test
    void testValidateUploadFile_Success() throws IOException {
        byte[] pdfBytes = createSamplePdfBytes("John Doe - Software Engineer with 5 years experience in Java, Spring Boot, and PostgreSQL.");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                pdfBytes
        );

        assertDoesNotThrow(() -> service.validateUploadFile(file));
    }

    @Test
    void testValidateUploadFile_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);
        InvalidResumeException ex = assertThrows(InvalidResumeException.class, () -> service.validateUploadFile(file));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void testValidateUploadFile_NonPdfExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.docx", "application/pdf", "some content".getBytes(StandardCharsets.UTF_8));
        InvalidResumeException ex = assertThrows(InvalidResumeException.class, () -> service.validateUploadFile(file));
        assertTrue(ex.getMessage().contains("Unsupported file type"));
    }

    @Test
    void testValidateUploadFile_InvalidMimeType() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "text/plain", "%PDF-fake".getBytes(StandardCharsets.UTF_8));
        InvalidResumeException ex = assertThrows(InvalidResumeException.class, () -> service.validateUploadFile(file));
        assertTrue(ex.getMessage().contains("Invalid MIME type"));
    }

    @Test
    void testValidatePdfMagicBytes_InvalidHeader() {
        byte[] fakeBytes = "NOT_A_PDF_FILE".getBytes(StandardCharsets.UTF_8);
        InvalidResumeException ex = assertThrows(InvalidResumeException.class, () -> service.validatePdfMagicBytes(fakeBytes));
        assertTrue(ex.getMessage().contains("signature verification failed"));
    }

    @Test
    void testExtractAndValidateText_ValidText() throws IOException {
        String longText = "Alice Developer - Staff Engineer with 8 years building distributed microservices using Java, Docker, and Kubernetes.";
        byte[] pdfBytes = createSamplePdfBytes(longText);

        String extracted = service.extractAndValidateText(pdfBytes);
        assertNotNull(extracted);
        assertTrue(extracted.contains("Alice Developer"));
        assertTrue(extracted.contains("Java"));
    }

    @Test
    void testExtractAndValidateText_InsufficientText() throws IOException {
        byte[] pdfBytes = createSamplePdfBytes("Hi"); // Less than 50 chars
        InvalidResumeException ex = assertThrows(InvalidResumeException.class, () -> service.extractAndValidateText(pdfBytes));
        assertTrue(ex.getMessage().contains("less than 50 characters detected"));
    }

    @Test
    void testNormalizeExtractedText() {
        String raw = "Jane\u0000 Doe\r\n\r\n\r\n\r\nExperience:    Engineer\n\nSkills: Java";
        String normalized = service.normalizeExtractedText(raw);
        assertFalse(normalized.contains("\u0000"));
        assertFalse(normalized.contains("\r\n"));
        assertTrue(normalized.contains("Jane Doe"));
    }
}
