package com.printwithai;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class SampleDocumentGenerator {

    @Test
    public void generateSampleDocuments() throws IOException {
        File samplesDir = new File("..\\samples");
        if (!samplesDir.exists()) {
            samplesDir = new File("samples");
        }
        if (!samplesDir.exists()) {
            samplesDir.mkdirs();
        }

        // 1. Generate Tahsil Farmer Application (3 pages: Page 1-2 full text, Page 3 affidavit note with low text)
        File tahsilPdf = new File(samplesDir, "Tahsil_Farmer_Application.pdf");
        try (PDDocument doc = new PDDocument()) {
            // Page 1: Government Tahsil Application Header & Bio-Data
            PDPage page1 = new PDPage();
            doc.addPage(page1);
            try (PDPageContentStream cos = new PDPageContentStream(doc, page1)) {
                cos.beginText();
                cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                cos.newLineAtOffset(50, 750);
                cos.showText("GOVERNMENT OF MAHARASHTRA - TAHSIL OFFICE, HAVELI");
                cos.newLineAtOffset(0, -30);
                cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cos.showText("Application for Land Record Extract (7/12 & 8A Utara Verification)");
                cos.newLineAtOffset(0, -25);
                cos.showText("Applicant Name: Ramesh Dnyandev Patil | Village: Khed Shivapur | District: Pune");
                cos.newLineAtOffset(0, -20);
                cos.showText("Subject: Request for certified copy of agricultural land tenancy certificate.");
                cos.newLineAtOffset(0, -20);
                cos.showText("Survey Gat No: 412/2B, Area: 2 Hectares 40 Gunthas. Verified by Village Talathi.");
                cos.newLineAtOffset(0, -20);
                cos.showText("This application is submitted via the Print With AI automated document kiosk.");
                cos.endText();
            }

            // Page 2: Crop Details & Survey Verification
            PDPage page2 = new PDPage();
            doc.addPage(page2);
            try (PDPageContentStream cos = new PDPageContentStream(doc, page2)) {
                cos.beginText();
                cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                cos.newLineAtOffset(50, 750);
                cos.showText("SCHEDULE OF CULTIVATION AND REVENUE PARTICULARS");
                cos.newLineAtOffset(0, -30);
                cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cos.showText("Crop Season: Kharif 2026 | Sown Area: Soybean (1.20 Ha), Sugarcane (1.20 Ha)");
                cos.newLineAtOffset(0, -25);
                cos.showText("Irrigation Source: Neera Left Bank Canal and Dug Well with 5HP Solar Pump.");
                cos.newLineAtOffset(0, -20);
                cos.showText("Bank No-Objection Certificate issued by District Central Cooperative Bank.");
                cos.newLineAtOffset(0, -20);
                cos.showText("Declaration: All furnished information is accurate under Maharashtra Land Revenue Code.");
                cos.endText();
            }

            // Page 3: Blank/Affidavit Endorsement Page (triggers AI Blank Page Notice!)
            PDPage page3 = new PDPage();
            doc.addPage(page3);
            try (PDPageContentStream cos = new PDPageContentStream(doc, page3)) {
                cos.beginText();
                cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 10);
                cos.newLineAtOffset(50, 750);
                cos.showText("[Page intentionally left for Talathi Seal and Signature Stamp]");
                cos.endText();
            }

            doc.save(tahsilPdf);
            System.out.println("Generated sample Tahsil PDF: " + tahsilPdf.getAbsolutePath());
        }

        // 2. Generate Student Project Report (5 pages)
        File collegePdf = new File(samplesDir, "Student_Project_Report.pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 1; i <= 5; i++) {
                PDPage p = new PDPage();
                doc.addPage(p);
                try (PDPageContentStream cos = new PDPageContentStream(doc, p)) {
                    cos.beginText();
                    cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                    cos.newLineAtOffset(50, 750);
                    cos.showText("PRINT WITH AI: SMART SELF-SERVICE DOCUMENT KIOSK - CHAPTER " + i);
                    cos.newLineAtOffset(0, -30);
                    cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cos.showText("Final Year B.Tech Engineering Project Submission | Savitribai Phule Pune University");
                    cos.newLineAtOffset(0, -25);
                    cos.showText("Project Team: Full Stack Java Development & Architecture Group");
                    cos.newLineAtOffset(0, -20);
                    cos.showText("Topic: Self-Service QR Kiosk, Apache PDFBox Processing, and Simulated Workflows.");
                    cos.endText();
                }
            }
            doc.save(collegePdf);
            System.out.println("Generated sample College PDF: " + collegePdf.getAbsolutePath());
        }
    }
}
