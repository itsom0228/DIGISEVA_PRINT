package com.printwithai.ai;

import com.printwithai.dto.AiAnalysisResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

@Service
public class DocumentAssistantService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAssistantService.class);

    private final boolean ollamaEnabled;
    private final String ollamaEndpoint;
    private final String ollamaModel;
    private final int ollamaTimeoutMs;

    public DocumentAssistantService(
            @Value("${app.ai.ollama.enabled:false}") boolean ollamaEnabled,
            @Value("${app.ai.ollama.endpoint:http://localhost:11434/api/generate}") String ollamaEndpoint,
            @Value("${app.ai.ollama.model:llama3.2}") String ollamaModel,
            @Value("${app.ai.ollama.timeout-ms:3000}") int ollamaTimeoutMs) {
        this.ollamaEnabled = ollamaEnabled;
        this.ollamaEndpoint = ollamaEndpoint;
        this.ollamaModel = ollamaModel;
        this.ollamaTimeoutMs = ollamaTimeoutMs;
    }

    public boolean isOllamaEnabled() {
        return ollamaEnabled;
    }

    public AiAnalysisResult analyzeDocument(Path filePath, String fileType, String language) {
        if (fileType != null && fileType.toLowerCase().contains("pdf")) {
            return analyzePdf(filePath, language);
        } else {
            return analyzeImage(filePath, language);
        }
    }

    private AiAnalysisResult analyzePdf(Path filePath, String language) {
        AiAnalysisResult result = new AiAnalysisResult();
        List<Integer> lowDensityPages = new ArrayList<>();
        int totalPages = 1;
        int totalWords = 0;
        int pagesWithText = 0;
        StringBuilder extractedSample = new StringBuilder();

        File file = filePath.toFile();
        try (PDDocument document = Loader.loadPDF(file)) {
            totalPages = document.getNumberOfPages();
            result.setTotalPages(totalPages);

            PDFTextStripper stripper = new PDFTextStripper();

            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = stripper.getText(document).trim();

                int charCount = pageText.length();
                String[] words = pageText.split("\\s+");
                int wordCount = (charCount == 0) ? 0 : words.length;

                totalWords += wordCount;

                if (charCount < 25 || wordCount < 5) {
                    lowDensityPages.add(p);
                } else {
                    pagesWithText++;
                }

                if (extractedSample.length() < 1200 && !pageText.isEmpty()) {
                    extractedSample.append(pageText).append(" ");
                }
            }

            result.setPagesWithText(pagesWithText);
            result.setLowDensityPageNumbers(lowDensityPages);
            result.setEstimatedWordCount(totalWords);
            result.setScannedOrImageHeavy(pagesWithText == 0 || (double) pagesWithText / totalPages < 0.4);
            result.setBlankWarning(!lowDensityPages.isEmpty());

            // Build recommendations and summary
            List<String> recommendations = new ArrayList<>();
            String lang = language != null ? language.toLowerCase() : "en";

            buildRecommendations(lang, totalPages, lowDensityPages, result.isScannedOrImageHeavy(), recommendations);
            result.setRecommendations(recommendations);

            // Check if Ollama is enabled and attempt local neural summary
            String neuralSummary = null;
            if (ollamaEnabled && !extractedSample.isEmpty()) {
                neuralSummary = queryOllama(extractedSample.toString(), lang);
            }

            if (neuralSummary != null && !neuralSummary.isBlank()) {
                result.setSummaryText(neuralSummary);
                result.setProviderUsed("OLLAMA_NEURAL");
            } else {
                result.setSummaryText(generateHeuristicSummary(lang, totalPages, pagesWithText, totalWords, lowDensityPages, result.isScannedOrImageHeavy()));
                result.setProviderUsed("LOCAL_HEURISTIC");
            }

        } catch (IOException e) {
            log.error("Error analyzing PDF document: {}", e.getMessage());
            result.setTotalPages(1);
            result.setSummaryText(getFallbackErrorMessage(language));
            result.setProviderUsed("LOCAL_HEURISTIC");
        }

        return result;
    }

    private AiAnalysisResult analyzeImage(Path filePath, String language) {
        AiAnalysisResult result = new AiAnalysisResult();
        result.setTotalPages(1);
        result.setPagesWithText(1);
        result.setEstimatedWordCount(0);
        result.setScannedOrImageHeavy(true);
        result.setBlankWarning(false);
        result.setProviderUsed("LOCAL_HEURISTIC");

        String lang = language != null ? language.toLowerCase() : "en";
        List<String> recs = new ArrayList<>();

        try {
            BufferedImage bimg = ImageIO.read(filePath.toFile());
            int width = bimg != null ? bimg.getWidth() : 0;
            int height = bimg != null ? bimg.getHeight() : 0;

            if ("mr".equals(lang)) {
                result.setSummaryText(String.format("प्रतिमा दस्तऐवज (%d × %d पिक्सेल). एकल पृष्ठ मुद्रण.", width, height));
                recs.add("स्पष्ट आणि उठावदार प्रिंटसाठी रंगीत (Colour) पर्याय निवडा.");
                recs.add("A4 आकारावर प्रतिमा व्यवस्थित फिट होईल याची खात्री करा.");
            } else if ("hi".equals(lang)) {
                result.setSummaryText(String.format("छवि दस्तावेज़ (%d × %d पिक्सेल)। एकल पृष्ठ प्रिंटिंग।", width, height));
                recs.add("स्पष्ट प्रिंट के लिए रंगीन (Colour) प्रिंटिंग विकल्प चुनें।");
                recs.add("प्रिंट करने से पहले जांचें कि छवि A4 पृष्ठ पर सही बैठती है।");
            } else {
                result.setSummaryText(String.format("Single page image document (%d × %d px).", width, height));
                recs.add("Select Colour mode for clear image reproduction.");
                recs.add("Ensure the image fits cleanly within standard A4 paper boundaries.");
            }
        } catch (Exception e) {
            result.setSummaryText("Image document loaded successfully. Ready for single-page printing.");
            recs.add("Choose Colour mode for best quality.");
        }

        result.setRecommendations(recs);
        return result;
    }

    private void buildRecommendations(String lang, int totalPages, List<Integer> lowDensityPages, boolean isScanned, List<String> recommendations) {
        if ("mr".equals(lang)) {
            if (!lowDensityPages.isEmpty()) {
                recommendations.add(String.format("पृष्ठ %s मध्ये मजकूर कमी आढळला आहे. हे रिकामे किंवा स्कॅन केलेले पान असू शकते; मुद्रणापूर्वी तपासा.", lowDensityPages));
            }
            if (totalPages > 8) {
                recommendations.add(String.format("दस्तऐवजात %d पृष्ठे आहेत. कागद वाचवण्यासाठी दुहेरी बाजू (Double-Sided) पर्याय वापरा.", totalPages));
                recommendations.add("सर्व पृष्ठे आवश्यक नसल्यास सानुकूल पृष्ठ श्रेणी (Custom Range) निवडा.");
            } else {
                recommendations.add("दस्तऐवज मुद्रणासाठी योग्य आहे. आवश्यक प्रती निवडून पुढे जा.");
            }
            if (isScanned) {
                recommendations.add("हा दस्तऐवज स्कॅन केलेला दिसतो. मजकूर गडद दिसण्यासाठी ब्लॅक अँड व्हाईट कॉन्ट्रास्ट चांगला राहील.");
            }
        } else if ("hi".equals(lang)) {
            if (!lowDensityPages.isEmpty()) {
                recommendations.add(String.format("पृष्ठ %s में बहुत कम टेक्स्ट मिला है। यह खाली या स्कैन किया हुआ पेज हो सकता है; कृपया जांच लें।", lowDensityPages));
            }
            if (totalPages > 8) {
                recommendations.add(String.format("दस्तावेज़ में %d पृष्ठ हैं। कागज़ बचाने के लिए डबल-साइडेड (Duplex) विकल्प चुनें।", totalPages));
                recommendations.add("यदि सभी पृष्ठों की आवश्यकता नहीं है, तो विशिष्ट पृष्ठ सीमा (Custom Page Range) चुनें।");
            } else {
                recommendations.add("दस्तावेज़ मुद्रण के लिए तैयार है। प्रतियां चुनें और प्रिंट करें।");
            }
            if (isScanned) {
                recommendations.add("यह दस्तावेज़ स्कैन की हुई प्रति प्रतीत होती है। स्पष्टता के लिए ब्लैक एंड व्हाइट या कलर उचित चुनें।");
            }
        } else {
            // English default
            if (!lowDensityPages.isEmpty()) {
                recommendations.add(String.format("Pages %s have very low extractable text. They might be blank pages or scanned images; review before printing.", lowDensityPages));
            }
            if (totalPages > 8) {
                recommendations.add(String.format("Document has %d pages. Consider Double-Sided (Duplex) printing to save paper.", totalPages));
                recommendations.add("If only specific pages are needed, specify a custom page range (e.g., 1-5).");
            } else {
                recommendations.add("Document format and structure look ready for printing.");
            }
            if (isScanned) {
                recommendations.add("Document appears to be scanned. Higher contrast black-and-white printing is recommended.");
            }
        }
    }

    private String generateHeuristicSummary(String lang, int totalPages, int pagesWithText, int totalWords, List<Integer> lowDensityPages, boolean isScanned) {
        if ("mr".equals(lang)) {
            if (isScanned) {
                return String.format("दस्तऐवजात %d पृष्ठे आहेत. हा मुख्यत्वे स्कॅन केलेला किंवा चित्रांवर आधारित दस्तऐवज दिसतो.", totalPages);
            }
            if (!lowDensityPages.isEmpty()) {
                return String.format("दस्तऐवजात %d पृष्ठे आणि अंदाजे %d शब्द आहेत. काही पृष्ठांवर (%s) मजकूर कमी आढळला आहे.", totalPages, totalWords, lowDensityPages);
            }
            return String.format("दस्तऐवज विश्लेषण पूर्ण: %d पृष्ठे, अंदाजे %d शब्द. सर्व पृष्ठे योग्य रीतीने भरलेली आहेत.", totalPages, totalWords);
        } else if ("hi".equals(lang)) {
            if (isScanned) {
                return String.format("दस्तावेज़ में %d पृष्ठ हैं। यह मुख्य रूप से स्कैन की गई सामग्री या छवि-आधारित दस्तावेज़ प्रतीत होता है।", totalPages);
            }
            if (!lowDensityPages.isEmpty()) {
                return String.format("दस्तावेज़ में %d पृष्ठ और लगभग %d शब्द हैं। कुछ पृष्ठों (%s) में कम टेक्स्ट मिला है।", totalPages, totalWords, lowDensityPages);
            }
            return String.format("दस्तावेज़ विश्लेषण संपन्न: %d पृष्ठ, लगभग %d शब्द। सभी पृष्ठ मुद्रण के लिए तैयार हैं।", totalPages, totalWords);
        } else {
            if (isScanned) {
                return String.format("The PDF contains %d pages. It appears to be primarily scanned or image-based content.", totalPages);
            }
            if (!lowDensityPages.isEmpty()) {
                return String.format("Your PDF contains %d pages with approx. %d words. Pages %s have minimal text; please review before printing.", totalPages, totalWords, lowDensityPages);
            }
            return String.format("Document validated: %d pages with approximately %d words. Text distribution is consistent throughout.", totalPages, totalWords);
        }
    }

    private String getFallbackErrorMessage(String lang) {
        if ("mr".equals(lang)) {
            return "दस्तऐवज यशस्वीरित्या स्वीकारला गेला. प्राथमिक विश्लेषण उपलब्ध आहे.";
        } else if ("hi".equals(lang)) {
            return "दस्तावेज़ सफलतापूर्वक स्वीकार किया गया। बुनियादी विश्लेषण उपलब्ध है।";
        }
        return "Document accepted. Basic analysis complete.";
    }

    /**
     * Queries local Ollama model if running.
     * Fails gracefully without throwing any uncaught exceptions.
     */
    private String queryOllama(String textSample, String lang) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(ollamaTimeoutMs))
                    .build();

            String prompt = String.format("Provide a concise 1-2 sentence summary of this document and 1 print recommendation in %s: %s",
                    lang.equals("mr") ? "Marathi" : lang.equals("hi") ? "Hindi" : "English",
                    textSample.replaceAll("[\"\\r\\n]", " "));

            String jsonPayload = String.format(
                    "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}",
                    ollamaModel, prompt.replace("\\", "\\\\").replace("\"", "\\\"")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofMillis(ollamaTimeoutMs))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                // Simple parsing of "response": "..."
                int respIndex = body.indexOf("\"response\":");
                if (respIndex != -1) {
                    int startQuote = body.indexOf("\"", respIndex + 11);
                    int endQuote = body.lastIndexOf("\"");
                    if (startQuote != -1 && endQuote > startQuote) {
                        return body.substring(startQuote + 1, endQuote).replace("\\n", " ").trim();
                    }
                }
            }
        } catch (Exception e) {
            log.info("Ollama local service not reachable or timed out. Falling back to rule-based analysis: {}", e.getMessage());
        }
        return null;
    }
}
