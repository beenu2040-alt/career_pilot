package com.careerpilot.service;

import com.careerpilot.exception.InvalidResumeException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class PdfValidationService {

    private static final Logger log = LoggerFactory.getLogger(PdfValidationService.class);

    // Magic bytes for PDF: "%PDF-" in ASCII (0x25, 0x50, 0x44, 0x46, 0x2D)
    private static final byte[] PDF_MAGIC_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D};
    private static final int MIN_EXTRACTED_CHARS = 50;
    private static final int MAX_EXTRACTED_CHARS = 30000;

    @Value("${careerpilot.resume.max-file-size:10485760}")
    private long maxFileSize;

    public void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidResumeException("No resume file was uploaded or file is empty.");
        }

        if (file.getSize() > maxFileSize) {
            throw new InvalidResumeException("File size exceeds the allowed limit of " + (maxFileSize / (1024 * 1024)) + "MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new InvalidResumeException("Unsupported file type. Please upload a valid .pdf document.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf")
                && !contentType.equalsIgnoreCase("application/x-pdf")) {
            throw new InvalidResumeException("Invalid MIME type: '" + contentType + "'. Only application/pdf is accepted.");
        }

        try {
            byte[] bytes = file.getBytes();
            validatePdfMagicBytes(bytes);
        } catch (IOException e) {
            throw new InvalidResumeException("Failed to read uploaded resume file bytes.", e);
        }
    }

    public void validatePdfMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < PDF_MAGIC_BYTES.length) {
            throw new InvalidResumeException("File is too small to be a valid PDF document.");
        }

        for (int i = 0; i < PDF_MAGIC_BYTES.length; i++) {
            if (bytes[i] != PDF_MAGIC_BYTES[i]) {
                throw new InvalidResumeException("File signature verification failed. The uploaded file is not a genuine PDF document.");
            }
        }
    }

    public String extractAndValidateText(byte[] pdfBytes) {
        validatePdfMagicBytes(pdfBytes);

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
            if (document.isEncrypted()) {
                throw new InvalidResumeException("Encrypted or password-protected PDF resumes are not supported. Please upload an unprotected PDF.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String rawText = stripper.getText(document);

            if (rawText == null || rawText.trim().length() < MIN_EXTRACTED_CHARS) {
                throw new InvalidResumeException("Unable to extract sufficient text content from this PDF (less than " +
                        MIN_EXTRACTED_CHARS + " characters detected). Please ensure your resume is not a flat image or scanned photo without OCR.");
            }

            return normalizeExtractedText(rawText);
        } catch (InvalidResumeException ire) {
            throw ire;
        } catch (Exception e) {
            log.warn("PDFBox parsing failed: {}", e.getMessage());
            throw new InvalidResumeException("Malformed or unreadable PDF document: " + e.getMessage(), e);
        }
    }

    public String normalizeExtractedText(String text) {
        if (text == null) {
            return "";
        }

        // Replace null characters and non-printable control characters (except newline, carriage return, tab)
        String cleaned = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        // Collapse excessive newlines (more than 3 into 2) and tabs/spaces
        cleaned = cleaned.replaceAll("[ \\t]+", " ");
        cleaned = cleaned.replaceAll("\\r\\n", "\n");
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        cleaned = cleaned.trim();

        // Enforce safe truncation limit
        if (cleaned.length() > MAX_EXTRACTED_CHARS) {
            log.info("Extracted resume text exceeded {} characters. Safely truncating.", MAX_EXTRACTED_CHARS);
            cleaned = cleaned.substring(0, MAX_EXTRACTED_CHARS);
        }

        return cleaned;
    }
}
