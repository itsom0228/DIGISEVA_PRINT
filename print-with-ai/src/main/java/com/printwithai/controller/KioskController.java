package com.printwithai.controller;

import com.printwithai.entity.Kiosk;
import com.printwithai.service.KioskService;
import com.printwithai.service.QrCodeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/kiosk")
public class KioskController {

    private final KioskService kioskService;
    private final QrCodeService qrCodeService;
    private final com.printwithai.service.PrintJobService printJobService;

    public KioskController(KioskService kioskService, QrCodeService qrCodeService, com.printwithai.service.PrintJobService printJobService) {
        this.kioskService = kioskService;
        this.qrCodeService = qrCodeService;
        this.printJobService = printJobService;
    }

    @GetMapping("/display")
    public String kioskDisplay(@org.springframework.web.bind.annotation.RequestParam(value = "kioskId", defaultValue = "LAPTOP001") String kioskId,
                               jakarta.servlet.http.HttpServletRequest request,
                               Model model) {
        if (!kioskService.isValidKiosk(kioskId)) {
            // If LAPTOP001 not yet in db, re-seed or default to first
            kioskService.seedInitialKiosks();
        }

        Kiosk kiosk = kioskService.isValidKiosk(kioskId) ? kioskService.getKioskById(kioskId) : kioskService.getAllKiosks().get(0);
        String kioskUrl = qrCodeService.getKioskUrl(kiosk.getKioskId());

        // Dynamic LAN IP fallback: If opened via IP or if app.base-url is localhost, adapt QR to match the accessed host
        if (request != null) {
            String serverName = request.getServerName();
            if (serverName != null && !serverName.equals("localhost") && !serverName.equals("127.0.0.1")) {
                int port = request.getServerPort();
                String scheme = request.getScheme();
                String hostBase = scheme + "://" + serverName + (port == 80 || port == 443 ? "" : ":" + port);
                kioskUrl = hostBase + "/print/new?kioskId=" + kiosk.getKioskId();
            }
        }

        String qrBase64 = qrCodeService.generateQrCodeBase64(kioskUrl, 350, 350);

        model.addAttribute("kiosk", kiosk);
        model.addAttribute("qrBase64", qrBase64);
        model.addAttribute("kioskUrl", kioskUrl);
        return "kiosk/display";
    }

    @org.springframework.web.bind.annotation.GetMapping("/api/status/{kioskId}")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<com.printwithai.dto.KioskDisplayStatusDto> getKioskLiveStatus(
            @PathVariable("kioskId") String kioskId) {
        if (!kioskService.isValidKiosk(kioskId)) {
            return ResponseEntity.notFound().build();
        }
        com.printwithai.dto.KioskDisplayStatusDto status = printJobService.getKioskDisplayStatus(kioskId);
        return ResponseEntity.ok(status);
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/reset/{kioskId}")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<java.util.Map<String, String>> resetKioskDisplay(
            @PathVariable("kioskId") String kioskId) {
        printJobService.resetKioskDisplay(kioskId);
        return ResponseEntity.ok(java.util.Map.of("status", "RESET", "kioskId", kioskId));
    }

    @GetMapping
    public String listKiosks(Model model) {
        model.addAttribute("kiosks", kioskService.getAllKiosks());
        return "kiosk/list";
    }

    @GetMapping("/{kioskId}")
    public String viewKioskDetails(@PathVariable("kioskId") String kioskId, Model model) {
        Kiosk kiosk = kioskService.getKioskById(kioskId);
        String qrBase64 = qrCodeService.generateKioskQrCodeBase64(kioskId);
        String kioskUrl = qrCodeService.getKioskUrl(kioskId);

        model.addAttribute("kiosk", kiosk);
        model.addAttribute("qrBase64", qrBase64);
        model.addAttribute("kioskUrl", kioskUrl);
        return "kiosk/details";
    }

    @GetMapping("/qr/{kioskId}")
    public ResponseEntity<byte[]> getKioskQrImage(@PathVariable("kioskId") String kioskId) {
        byte[] qrBytes = qrCodeService.generateKioskQrCode(kioskId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrBytes);
    }

    @GetMapping("/{kioskId}/download-qr")
    public ResponseEntity<byte[]> downloadKioskQr(@PathVariable("kioskId") String kioskId) {
        byte[] qrBytes = qrCodeService.generateKioskQrCode(kioskId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"kiosk-" + kioskId + "-qr.png\"")
                .body(qrBytes);
    }
}
