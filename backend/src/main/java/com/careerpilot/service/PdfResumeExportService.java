package com.careerpilot.service;

import com.careerpilot.dto.TailoredExperienceDto;
import com.careerpilot.dto.TailoredProjectDto;
import com.careerpilot.dto.TailoredResumeContentDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfResumeExportService {

    private static final Logger log = LoggerFactory.getLogger(PdfResumeExportService.class);

    private static final float MARGIN_LEFT = 50f;
    private static final float MARGIN_RIGHT = 50f;
    private static final float MARGIN_TOP = 750f;
    private static final float MARGIN_BOTTOM = 50f;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

    public byte[] generateResumePdf(TailoredResumeContentDto content, String targetRole) {
        try (PDDocument document = new PDDocument()) {
            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            PdfContext ctx = new PdfContext(document, fontBold, fontRegular, fontOblique);
            ctx.newPage();

            // 1. HEADER: Candidate Name
            String name = (content.candidateName() != null && !content.candidateName().isBlank())
                    ? content.candidateName()
                    : "Candidate Resume";
            ctx.drawText(name, fontBold, 18, 0.1f, 0.15f, 0.25f);
            ctx.y -= 6;

            // Target Role subtitle
            String effectiveRole = (targetRole != null && !targetRole.isBlank())
                    ? targetRole
                    : "Software Engineer";
            ctx.drawText(effectiveRole, fontBold, 12, 0.2f, 0.35f, 0.55f);
            ctx.y -= 4;

            // Contact info
            List<String> contactParts = new ArrayList<>();
            if (content.email() != null && !content.email().isBlank()) contactParts.add(content.email());
            if (content.phone() != null && !content.phone().isBlank()) contactParts.add(content.phone());
            if (content.location() != null && !content.location().isBlank()) contactParts.add(content.location());
            if (!contactParts.isEmpty()) {
                ctx.drawText(String.join("  |  ", contactParts), fontRegular, 9, 0.35f, 0.4f, 0.45f);
            }
            ctx.y -= 10;

            // Horizontal rule
            ctx.drawHorizontalRule(0.8f, 0.82f, 0.85f);
            ctx.y -= 16;

            // 2. PROFESSIONAL SUMMARY
            if (content.professionalSummary() != null && !content.professionalSummary().isBlank()) {
                ctx.drawSectionHeader("PROFESSIONAL SUMMARY");
                ctx.drawParagraph(content.professionalSummary(), fontRegular, 9.5f, 13f, 0);
                ctx.y -= 14;
            }

            // 3. CORE TECHNICAL COMPETENCIES / SKILLS
            if (content.skills() != null && !content.skills().isEmpty()) {
                ctx.drawSectionHeader("CORE COMPETENCIES & TECHNICAL SKILLS");
                String skillsLine = String.join("  •  ", content.skills());
                ctx.drawParagraph(skillsLine, fontRegular, 9f, 12.5f, 0);
                ctx.y -= 14;
            }

            // 4. PROFESSIONAL EXPERIENCE
            if (content.experience() != null && !content.experience().isEmpty()) {
                ctx.drawSectionHeader("PROFESSIONAL EXPERIENCE");
                for (TailoredExperienceDto exp : content.experience()) {
                    ctx.checkPageBreak(50);
                    String roleHeader = (exp.roleTitle() != null ? exp.roleTitle() : "Role") +
                            (exp.company() != null ? " — " + exp.company() : "");
                    ctx.drawText(roleHeader, fontBold, 10.5f, 0.15f, 0.2f, 0.25f);

                    if (exp.duration() != null && !exp.duration().isBlank()) {
                        ctx.y -= 2;
                        ctx.drawText(exp.duration(), fontOblique, 8.5f, 0.4f, 0.45f, 0.5f);
                    }
                    ctx.y -= 4;

                    if (exp.bullets() != null) {
                        for (String bullet : exp.bullets()) {
                            ctx.drawBulletPoint(bullet, fontRegular, 9f, 12.5f);
                        }
                    }
                    ctx.y -= 10;
                }
            }

            // 5. TECHNICAL PROJECTS
            if (content.projects() != null && !content.projects().isEmpty()) {
                ctx.drawSectionHeader("FEATURED TECHNICAL PROJECTS");
                for (TailoredProjectDto prj : content.projects()) {
                    ctx.checkPageBreak(40);
                    String title = prj.title() != null ? prj.title() : "Project";
                    ctx.drawText(title, fontBold, 10f, 0.15f, 0.2f, 0.25f);

                    if (prj.techStack() != null && !prj.techStack().isEmpty()) {
                        ctx.y -= 2;
                        ctx.drawText("Tech Stack: " + String.join(", ", prj.techStack()), fontOblique, 8.5f, 0.35f, 0.45f, 0.55f);
                    }
                    ctx.y -= 3;

                    if (prj.description() != null && !prj.description().isBlank()) {
                        ctx.drawParagraph(prj.description(), fontRegular, 9f, 12f, 8);
                    }

                    if (prj.bullets() != null) {
                        for (String b : prj.bullets()) {
                            ctx.drawBulletPoint(b, fontRegular, 9f, 12f);
                        }
                    }
                    ctx.y -= 8;
                }
            }

            // 6. EDUCATION
            if (content.education() != null && !content.education().isEmpty()) {
                ctx.drawSectionHeader("EDUCATION & CREDENTIALS");
                for (String edu : content.education()) {
                    ctx.drawBulletPoint(edu, fontRegular, 9f, 12.5f);
                }
                ctx.y -= 10;
            }

            // Finish up stream
            ctx.closeCurrentStream();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate tailored resume PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate tailored resume PDF: " + e.getMessage(), e);
        }
    }

    private static String sanitizeForPdf(String text) {
        if (text == null) return "";
        return text
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .replace("\u2013", "-")
                .replace("\u2014", "--")
                .replace("\u2022", "*")
                .replaceAll("[^\\x00-\\x7F]", " ");
    }

    private static class PdfContext {
        final PDDocument document;
        final PDFont fontBold;
        final PDFont fontRegular;
        final PDFont fontOblique;
        PDPageContentStream stream;
        float y;

        PdfContext(PDDocument document, PDFont fontBold, PDFont fontRegular, PDFont fontOblique) {
            this.document = document;
            this.fontBold = fontBold;
            this.fontRegular = fontRegular;
            this.fontOblique = fontOblique;
        }

        void newPage() throws IOException {
            closeCurrentStream();
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = MARGIN_TOP;
        }

        void checkPageBreak(float spaceRequired) throws IOException {
            if (y - spaceRequired < MARGIN_BOTTOM) {
                newPage();
            }
        }

        void closeCurrentStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        void drawText(String text, PDFont font, float fontSize, float r, float g, float b) throws IOException {
            checkPageBreak(fontSize + 6);
            String safe = sanitizeForPdf(text);
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.setNonStrokingColor(r, g, b);
            stream.newLineAtOffset(MARGIN_LEFT, y);
            stream.showText(safe);
            stream.endText();
            y -= (fontSize + 3);
        }

        void drawHorizontalRule(float r, float g, float b) throws IOException {
            checkPageBreak(5);
            stream.setStrokingColor(r, g, b);
            stream.setLineWidth(0.8f);
            stream.moveTo(MARGIN_LEFT, y);
            stream.lineTo(MARGIN_LEFT + CONTENT_WIDTH, y);
            stream.stroke();
            y -= 4;
        }

        void drawSectionHeader(String title) throws IOException {
            checkPageBreak(25);
            drawText(title, fontBold, 10.5f, 0.1f, 0.25f, 0.45f);
            drawHorizontalRule(0.75f, 0.8f, 0.85f);
            y -= 4;
        }

        void drawParagraph(String text, PDFont font, float fontSize, float leading, float indent) throws IOException {
            String safe = sanitizeForPdf(text);
            List<String> lines = wrapText(safe, font, fontSize, CONTENT_WIDTH - indent);
            for (String line : lines) {
                checkPageBreak(leading);
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.setNonStrokingColor(0.2f, 0.2f, 0.25f);
                stream.newLineAtOffset(MARGIN_LEFT + indent, y);
                stream.showText(line);
                stream.endText();
                y -= leading;
            }
        }

        void drawBulletPoint(String text, PDFont font, float fontSize, float leading) throws IOException {
            String safe = sanitizeForPdf(text);
            float bulletIndent = 12f;
            List<String> lines = wrapText(safe, font, fontSize, CONTENT_WIDTH - bulletIndent);
            if (lines.isEmpty()) return;

            checkPageBreak(leading);
            // Draw bullet marker
            stream.beginText();
            stream.setFont(fontBold, fontSize);
            stream.setNonStrokingColor(0.15f, 0.35f, 0.6f);
            stream.newLineAtOffset(MARGIN_LEFT + 2, y);
            stream.showText("-");
            stream.endText();

            // First line
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.setNonStrokingColor(0.2f, 0.2f, 0.25f);
            stream.newLineAtOffset(MARGIN_LEFT + bulletIndent, y);
            stream.showText(lines.get(0));
            stream.endText();
            y -= leading;

            // Subsequent wrapped lines
            for (int i = 1; i < lines.size(); i++) {
                checkPageBreak(leading);
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.setNonStrokingColor(0.2f, 0.2f, 0.25f);
                stream.newLineAtOffset(MARGIN_LEFT + bulletIndent, y);
                stream.showText(lines.get(i));
                stream.endText();
                y -= leading;
            }
        }

        List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String[] words = text.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
                float width = font.getStringWidth(candidate) / 1000 * fontSize;
                if (width <= maxWidth) {
                    currentLine = new StringBuilder(candidate);
                } else {
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                    }
                    currentLine = new StringBuilder(word);
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
            return lines;
        }
    }
}
