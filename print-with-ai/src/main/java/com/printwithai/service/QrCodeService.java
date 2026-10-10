package com.printwithai.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrCodeService {

    private final String baseUrl;

    public QrCodeService(@Value("${app.base-url:http://localhost:8085}") String baseUrl) {
        String url = (baseUrl != null && !baseUrl.trim().isEmpty()) ? baseUrl.trim() : "http://localhost:8085";
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        this.baseUrl = url;
    }

    public String getKioskUrl(String kioskId) {
        return baseUrl + "/print/new?kioskId=" + kioskId;
    }

    public byte[] generateQrCodeImage(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR Code for content: " + text, e);
        }
    }

    public String generateQrCodeBase64(String text, int width, int height) {
        byte[] imageBytes = generateQrCodeImage(text, width, height);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public byte[] generateKioskQrCode(String kioskId) {
        String kioskUrl = getKioskUrl(kioskId);
        return generateQrCodeImage(kioskUrl, 350, 350);
    }

    public String generateKioskQrCodeBase64(String kioskId) {
        String kioskUrl = getKioskUrl(kioskId);
        return generateQrCodeBase64(kioskUrl, 350, 350);
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
