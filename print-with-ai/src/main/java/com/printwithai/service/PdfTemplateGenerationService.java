package com.printwithai.service;

import com.printwithai.entity.ApplicationTemplate;
import com.printwithai.dto.TemplateCustomizationDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfTemplateGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PdfTemplateGenerationService.class);

    private static final float MARGIN_LEFT = 50f;
    private static final float MARGIN_RIGHT = 50f;
    private static final float MARGIN_TOP = 60f;
    private static final float MARGIN_BOTTOM = 60f;
    private static final float FONT_SIZE_TITLE = 15f;
    private static final float FONT_SIZE_BODY = 11.5f;
    private static final float FONT_SIZE_SMALL = 9f;
    private static final float LINE_SPACING = 16f;

    public byte[] generateApplicationPdf(ApplicationTemplate template, TemplateCustomizationDto customization) {
        try (PDDocument document = new PDDocument()) {
            PDFont font = loadDevanagariFont(document);
            PDFont standardFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            String textContent = resolveTemplateText(template, customization);
            String[] paragraphs = textContent.split("\\r?\\n");

            PDRectangle mediaBox = PDRectangle.A4;
            float usableWidth = mediaBox.getWidth() - MARGIN_LEFT - MARGIN_RIGHT;

            List<List<String>> pagesLines = new ArrayList<>();
            List<String> currentLines = new ArrayList<>();
            float usableHeight = mediaBox.getHeight() - MARGIN_TOP - MARGIN_BOTTOM - 60f; // Leave space for headers/footers
            int maxLinesPerPage = (int) (usableHeight / LINE_SPACING);

            for (String para : paragraphs) {
                if (para.trim().isEmpty()) {
                    currentLines.add("");
                    if (currentLines.size() >= maxLinesPerPage) {
                        pagesLines.add(new ArrayList<>(currentLines));
                        currentLines.clear();
                    }
                    continue;
                }

                List<String> wrapped = wrapText(para, font, FONT_SIZE_BODY, usableWidth);
                for (String wLine : wrapped) {
                    currentLines.add(wLine);
                    if (currentLines.size() >= maxLinesPerPage) {
                        pagesLines.add(new ArrayList<>(currentLines));
                        currentLines.clear();
                    }
                }
            }
            if (!currentLines.isEmpty() || pagesLines.isEmpty()) {
                pagesLines.add(currentLines);
            }

            int totalPages = pagesLines.size();

            for (int pageIdx = 0; pageIdx < totalPages; pageIdx++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    // Header Bar (Brand & Title)
                    drawHeader(cs, font, template, pageIdx + 1, totalPages, mediaBox);

                    // Body Content
                    List<String> lines = pagesLines.get(pageIdx);
                    float y = mediaBox.getHeight() - MARGIN_TOP - 45f;

                    for (String line : lines) {
                        if (!line.isEmpty()) {
                            cs.beginText();
                            cs.setFont(font, FONT_SIZE_BODY);
                            cs.setNonStrokingColor(Color.DARK_GRAY);
                            cs.newLineAtOffset(MARGIN_LEFT, y);
                            cs.showText(sanitizeTextForFont(line, font));
                            cs.endText();
                        }
                        y -= LINE_SPACING;
                    }

                    // Footer Bar
                    drawFooter(cs, font, pageIdx + 1, totalPages, mediaBox);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate application PDF for template: {}", template.getTitle(), e);
            throw new RuntimeException("Error generating application PDF: " + e.getMessage(), e);
        }
    }

    private void drawHeader(PDPageContentStream cs, PDFont font, ApplicationTemplate template,
                            int pageNum, int totalPages, PDRectangle mediaBox) throws IOException {
        float width = mediaBox.getWidth();
        float topY = mediaBox.getHeight() - 35f;

        // Draw top accent line
        cs.setStrokingColor(new Color(37, 99, 235)); // Blue #2563eb
        cs.setLineWidth(2f);
        cs.moveTo(MARGIN_LEFT, topY - 15f);
        cs.lineTo(width - MARGIN_RIGHT, topY - 15f);
        cs.stroke();

        // Application Category & Title header
        cs.beginText();
        cs.setFont(font, FONT_SIZE_TITLE);
        cs.setNonStrokingColor(new Color(15, 23, 42)); // Slate 900
        cs.newLineAtOffset(MARGIN_LEFT, topY);
        String headerTitle = template.getTitle();
        if (headerTitle.length() > 45) {
            headerTitle = headerTitle.substring(0, 42) + "...";
        }
        cs.showText(sanitizeTextForFont(headerTitle, font));
        cs.endText();

        // Right side badge text
        String badge = (template.isOfficialTemplate() ? "OFFICIAL FORMAT" : "SAMPLE APPLICATION FORMAT");
        cs.beginText();
        cs.setFont(font, FONT_SIZE_SMALL);
        cs.setNonStrokingColor(new Color(100, 116, 139)); // Slate 500
        float badgeWidth = getTextWidth(badge, font, FONT_SIZE_SMALL);
        cs.newLineAtOffset(width - MARGIN_RIGHT - badgeWidth, topY);
        cs.showText(badge);
        cs.endText();
    }

    private void drawFooter(PDPageContentStream cs, PDFont font,
                            int pageNum, int totalPages, PDRectangle mediaBox) throws IOException {
        float width = mediaBox.getWidth();
        float footY = MARGIN_BOTTOM - 20f;

        // Bottom light divider line
        cs.setStrokingColor(new Color(226, 232, 240)); // Slate 200
        cs.setLineWidth(0.8f);
        cs.moveTo(MARGIN_LEFT, footY + 15f);
        cs.lineTo(width - MARGIN_RIGHT, footY + 15f);
        cs.stroke();

        // Disclaimer
        String disclaimer = "Print with AI | Standard format for institutional submission. Verify specific requirements with recipient office.";
        cs.beginText();
        cs.setFont(font, 8f);
        cs.setNonStrokingColor(new Color(148, 163, 184)); // Slate 400
        cs.newLineAtOffset(MARGIN_LEFT, footY);
        cs.showText(sanitizeTextForFont(disclaimer, font));
        cs.endText();

        // Page Number
        String pageStr = "Page " + pageNum + " of " + totalPages;
        cs.beginText();
        cs.setFont(font, 8.5f);
        cs.setNonStrokingColor(new Color(100, 116, 139));
        float pageStrWidth = getTextWidth(pageStr, font, 8.5f);
        cs.newLineAtOffset(width - MARGIN_RIGHT - pageStrWidth, footY);
        cs.showText(pageStr);
        cs.endText();
    }

    private String resolveTemplateText(ApplicationTemplate template, TemplateCustomizationDto c) {
        if (c != null && c.getCustomizedContent() != null && !c.getCustomizedContent().trim().isEmpty()) {
            return c.getCustomizedContent().trim();
        }

        String raw = template.getTemplateContent();
        if (c == null) {
            return raw;
        }

        String applicant = (c.getApplicantName() != null && !c.getApplicantName().trim().isEmpty()) ? c.getApplicantName().trim() : "[Student / Applicant Name]";
        String contact = (c.getContactNumber() != null && !c.getContactNumber().trim().isEmpty()) ? c.getContactNumber().trim() : "[Contact Number]";
        String address = (c.getAddress() != null && !c.getAddress().trim().isEmpty()) ? c.getAddress().trim() : "[Address / Department]";
        String recipient = (c.getRecipientTitle() != null && !c.getRecipientTitle().trim().isEmpty()) ? c.getRecipientTitle().trim() : "[Principal / Officer Title]";
        String office = (c.getOfficeName() != null && !c.getOfficeName().trim().isEmpty()) ? c.getOfficeName().trim() : "[College / Office Name]";
        String refNo = (c.getReferenceNumber() != null && !c.getReferenceNumber().trim().isEmpty()) ? c.getReferenceNumber().trim() : "[Roll No / Application No]";
        String purpose = (c.getPurpose() != null && !c.getPurpose().trim().isEmpty()) ? c.getPurpose().trim() : (template.getPurpose() != null ? template.getPurpose() : "[Purpose of Request]");
        String date = (c.getDate() != null && !c.getDate().trim().isEmpty()) ? c.getDate().trim() : LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // Replace common standard placeholders
        String processed = raw
                .replace("[Applicant Name]", applicant)
                .replace("[Student Name]", applicant)
                .replace("[Name of Applicant]", applicant)
                .replace("[अर्जदाराचे नाव]", applicant)
                .replace("[विद्यार्थ्याचे नाव]", applicant)
                .replace("[Contact Number]", contact)
                .replace("[Mobile Number]", contact)
                .replace("[मोबाईल क्र.]", contact)
                .replace("[संपर्क क्र.]", contact)
                .replace("[Address]", address)
                .replace("[पत्ता]", address)
                .replace("[Principal / Officer Title]", recipient)
                .replace("[Officer Title]", recipient)
                .replace("[The Principal]", recipient)
                .replace("[College Name]", office)
                .replace("[Office Name]", office)
                .replace("[महाविद्यालय / कार्यालय नाव]", office)
                .replace("[Roll Number]", refNo)
                .replace("[Roll No]", refNo)
                .replace("[हजेरी क्र. / नोंद क्र.]", refNo)
                .replace("[Application Number]", refNo)
                .replace("[Purpose]", purpose)
                .replace("[Purpose of Request]", purpose)
                .replace("[कारणास्तव / उद्देश]", purpose)
                .replace("[Date]", date)
                .replace("[दिनांक]", date);

        return processed;
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) {
        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
            float width = getTextWidth(candidate, font, fontSize);

            if (width <= maxWidth) {
                currentLine.append(currentLine.length() == 0 ? "" : " ").append(word);
            } else {
                if (currentLine.length() > 0) {
                    result.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Single word exceeds width, add as is
                    result.add(word);
                    currentLine = new StringBuilder();
                }
            }
        }

        if (currentLine.length() > 0) {
            result.add(currentLine.toString());
        }

        return result;
    }

    private float getTextWidth(String text, PDFont font, float fontSize) {
        try {
            return font.getStringWidth(sanitizeTextForFont(text, font)) / 1000f * fontSize;
        } catch (Exception e) {
            return text.length() * fontSize * 0.5f; // Fallback estimate
        }
    }

    private String sanitizeTextForFont(String text, PDFont font) {
        if (text == null) return "";
        // Remove unsupported non-printable control characters that might crash font encoders
        return text.replace("\t", "    ")
                   .replaceAll("[\\p{Cntrl}&&[^\r\n]]", "");
    }

    private PDFont loadDevanagariFont(PDDocument document) {
        // 1. Try bundled resources font (src/main/resources/fonts/mangal.ttf)
        try (InputStream is = getClass().getResourceAsStream("/fonts/mangal.ttf")) {
            if (is != null) {
                return PDType0Font.load(document, is, true);
            }
        } catch (Exception e) {
            log.warn("Could not load /fonts/mangal.ttf from classpath: {}", e.getMessage());
        }

        // 2. Try Windows Fonts directory directly
        File winMangal = new File("C:\\Windows\\Fonts\\mangal.ttf");
        if (winMangal.exists()) {
            try {
                return PDType0Font.load(document, winMangal);
            } catch (Exception e) {
                log.warn("Could not load font from C:\\Windows\\Fonts\\mangal.ttf: {}", e.getMessage());
            }
        }

        // 3. Try Nirmala font
        File winNirmala = new File("C:\\Windows\\Fonts\\Nirmala.ttc");
        if (winNirmala.exists()) {
            try {
                return PDType0Font.load(document, winNirmala);
            } catch (Exception ignored) {}
        }

        // 4. Fallback to standard Helvetica if Devanagari font is unavailable
        log.warn("Devanagari TrueType font not found. Falling back to Standard Helvetica font.");
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }
}
