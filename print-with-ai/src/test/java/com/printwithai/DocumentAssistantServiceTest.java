package com.printwithai;

import com.printwithai.ai.DocumentAssistantService;
import com.printwithai.dto.AiAnalysisResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DocumentAssistantServiceTest {

    @TempDir
    Path tempDir;

    private DocumentAssistantService assistantService;

    @BeforeEach
    void setUp() {
        // Ollama disabled for test
        assistantService = new DocumentAssistantService(false, "http://localhost:11434/api/generate", "llama3.2", 3000);
    }

    @Test
    @DisplayName("Analyze PDF with text page and low-density/blank page")
    void testAnalyzePdfWithBlankPage() throws IOException {
        // Create 2-page sample PDF: Page 1 with text, Page 2 empty
        File samplePdf = tempDir.resolve("test-doc.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            PDPage page1 = new PDPage();
            doc.addPage(page1);

            try (PDPageContentStream cos = new PDPageContentStream(doc, page1)) {
                cos.beginText();
                cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                cos.newLineAtOffset(100, 700);
                cos.showText("This is page one with sufficient text density for testing Print With AI platform.");
                cos.endText();
            }

            // Page 2: empty (blank page)
            PDPage page2 = new PDPage();
            doc.addPage(page2);

            doc.save(samplePdf);
        }

        // Test English
        AiAnalysisResult enResult = assistantService.analyzeDocument(samplePdf.toPath(), "application/pdf", "en");
        assertNotNull(enResult);
        assertEquals(2, enResult.getTotalPages());
        assertTrue(enResult.isBlankWarning());
        assertTrue(enResult.getLowDensityPageNumbers().contains(2));
        assertNotNull(enResult.getSummaryText());
        assertFalse(enResult.getRecommendations().isEmpty());
        assertEquals("LOCAL_HEURISTIC", enResult.getProviderUsed());

        // Test Marathi localization
        AiAnalysisResult mrResult = assistantService.analyzeDocument(samplePdf.toPath(), "application/pdf", "mr");
        assertNotNull(mrResult);
        assertTrue(mrResult.getSummaryText().contains("पृष्ठे"));

        // Test Hindi localization
        AiAnalysisResult hiResult = assistantService.analyzeDocument(samplePdf.toPath(), "application/pdf", "hi");
        assertNotNull(hiResult);
        assertTrue(hiResult.getSummaryText().contains("पृष्ठ"));
    }
}
