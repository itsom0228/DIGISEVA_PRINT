# 📄 PROJECT REPORT: PRINT WITH AI
## AI-Assisted Smart Printing and Document Kiosk

---

### ABSTRACT
In developing nations and rapidly digitalizing public service environments, access to paper printouts of legal applications, agricultural revenue records (such as 7/12 extract forms in Maharashtra), medical summaries, and academic assignments remains an everyday necessity. Traditional print shops, however, are burdened by long waiting times, lack of transparent pricing, and critical security and privacy vulnerabilities caused by customers handing over unlocked smartphones or transferring sensitive personal documents via messaging applications like WhatsApp or email to third-party operators.

**Print with AI** introduces a self-service, QR-activated smart printing kiosk software platform engineered to eliminate middleman intervention. Using their own smartphones, citizens scan a localized kiosk QR code, upload documents (PDF, JPG, PNG) securely, and receive automated AI Document Intelligence that calculates page counts, identifies potential blank or scanned pages, and recommends cost-effective printing options in Marathi, Hindi, or English. Customers review transparent rates, complete a simulated demo payment, and trigger an automated printing workflow. An administrative dashboard equipped with role-based security, KPI metrics, hardware logs, and a scheduled file-privacy purge completes the system. The platform is implemented using Java 17, Spring Boot 3.4.x, Spring Security, Apache PDFBox, ZXing, and Thymeleaf with a modern responsive SaaS design.

---

### 1. INTRODUCTION
Self-service automated kiosks have revolutionized banking (ATMs), ticketing (railways and airports), and fast-food ordering. However, personal document printing in government offices (Tahsil, Collectorate, Talathi offices), universities, hospitals, and community libraries has remained largely manual and operator-dependent.

**Print with AI** bridges this digital-physical gap by turning any network-connected printing terminal into a smart, self-service kiosk accessible through a standard smartphone browser without requiring mobile app installations.

---

### 2. PROBLEM STATEMENT
Current printing operations in public and educational spaces suffer from the following problems:
1. **Dependency on Third-Party Operators:** Citizens must depend on private shopkeepers or clerks who charge arbitrary rates and handle files manually.
2. **Data Privacy & Security Leakage:** Citizens frequently send sensitive identity cards (Aadhaar, PAN), land ownership titles, and personal affidavits over unencrypted chat apps or email, leaving permanent digital traces on third-party computers.
3. **Paper & Money Waste:** Users regularly print documents without realizing that certain pages are blank or that accidental portrait/landscape mismatches occurred.
4. **Queue Congestion:** Manual file handling and slow change-making during cash transactions create bottlenecks in busy public offices.
5. **Language Barrier:** Most desktop printing interfaces are in English, alienating rural citizens and first-time smartphone users.

---

### 3. OBJECTIVES
1. Develop a zero-touch QR-based entry system mapping smartphone sessions to specific kiosk hardware.
2. Build a secure document processing pipeline supporting PDF and image formats with strict server-side validation and path-traversal prevention.
3. Implement an AI Document Assistant providing heuristic analysis (page counting, text density estimation, blank page warnings) and optional local neural intelligence (Ollama) without relying on expensive paid APIs.
4. Provide trilingual internationalization (English, Marathi, Hindi) across all customer-facing workflows.
5. Implement server-side pricing using `BigDecimal` with demonstration rates and paper-saving duplex discounts.
6. Provide a simulated demo payment workflow (UPI QR and counter cash) with clear academic disclaimers.
7. Implement an asynchronous mock printer service simulating spooling, printing, and completion with live timeline tracking.
8. Deliver an administrative dashboard with Spring Security, BCrypt encryption, KPI metrics, and scheduled file-retention cleanup.

---

### 4. EXISTING SYSTEM AND LIMITATIONS

| Feature | Existing Manual Printing Shops | Proposed "Print with AI" Kiosk |
|---|---|---|
| **Access Model** | USB pen-drive or WhatsApp transfer to clerk | Zero-contact smartphone QR scan |
| **Document Privacy** | Files remain on shopkeeper's desktop | Stored with random UUID; purged after retention window |
| **Pricing Transparency** | Arbitrary prices decided by shopkeeper | Mathematical server-calculated formula using `BigDecimal` |
| **Document Assistance** | None; blank pages are printed and charged | AI warns about low text density / blank pages beforehand |
| **Multilingual Support** | English-only printer dialogues | Trilingual (English, Marathi, Hindi) |
| **Payment Options** | Cash change disputes | Instant simulated demo UPI QR or counter mode |
| **Tracking** | Manual inquiry with shopkeeper | Secure token-based public tracking timeline |

---

### 5. SYSTEM ARCHITECTURE

```
[ Citizen Smartphone ]
         │
         ▼ (Scans Kiosk QR: /print/new?kioskId=TAHSIL001)
┌─────────────────────────────────────────────────────────────┐
│                    PRINT WITH AI WEB APP                    │
│                                                             │
│  [ WebMvc / Thymeleaf / i18n: EN / MR / HI ]                │
│  [ Spring Security: CSRF & Role-Based Auth ]                │
└────────────────┬───────────────────────────┬────────────────┘
                 │                           │
                 ▼                           ▼
┌─────────────────────────────────┐ ┌─────────────────────────────────┐
│     CUSTOMER WORKFLOW           │ │       ADMIN WORKFLOW            │
│ • Secure Multipart Upload       │ │ • BCrypt Authentication         │
│ • Apache PDFBox Text Analysis   │ │ • KPI Metrics Cards             │
│ • Heuristic / Ollama Assistant  │ │ • Order Search & Filtering      │
│ • Backend Pricing (BigDecimal)  │ │ • Hardware Audit Logs           │
│ • Demo Payment (UPI / Cash)     │ │ • Manual / Scheduled Purge     │
└────────────────┬────────────────┘ └────────────────┬────────────────┘
                 │                                   │
                 ▼                                   ▼
┌─────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                          │
│ • PrintJobService    • StorageService (UUID Obfuscation)    │
│ • PricingService     • DocumentAssistantService             │
│ • QrCodeService      • MockPrinterService (Async Spooler)   │
│ • PaymentService     • FileCleanupService (@Scheduled)      │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│               DATA PERSISTENCE & STORAGE                    │
│ • Spring Data JPA & Hibernate 6                             │
│ • H2 In-Memory DB (Demo) / MySQL 8 (Production)             │
│ • Isolated Filesystem Directory: ./uploads/                 │
└─────────────────────────────────────────────────────────────┘
```

---

### 6. MODULE DESCRIPTIONS

#### Module A: Home & Kiosk Navigation
- Responsive landing page featuring high-contrast typography, glassmorphism cards, and a 30-second product summary.
- Trilingual language switcher (English, Marathi, Hindi) utilizing Spring Boot `CookieLocaleResolver` and `LocaleChangeInterceptor`.
- Showcase of demonstration kiosks (`TAHSIL001`, `COLLEGE001`, `HOSPITAL001`, `LIBRARY001`).

#### Module B: QR Code Engine (ZXing)
- Dynamic generation of QR matrices encoding kiosk endpoint URLs (e.g., `http://localhost:8085/print/new?kioskId=TAHSIL001`).
- Ability to download printable high-resolution PNG QR signs for physical kiosks.
- Strict backend validation ensuring the kiosk identifier exists before order creation.

#### Module C: Document Upload & Storage Security
- Client-side drag-and-drop dropzone with size and extension validation.
- Server-side MIME validation, path traversal sanitation, and storage outside web root (`./uploads/`).
- Filenames obfuscated via `UUID.randomUUID()`.

#### Module D: Document Intelligence (Apache PDFBox & Ollama)
- Extraction of PDF page counts and character/word distributions.
- Detection of low-density pages ($< 25$ characters) to warn users about accidental blank pages or image-only scanned documents.
- Optional integration with local Ollama neural model (`llama3.2`) with 3000ms timeout and automatic heuristic fallback.

#### Module E: Print Configuration & BigDecimal Pricing
- Configurable demonstration rates: B&W (₹2.00/page), Colour (₹8.00/page).
- Page range parsing supporting expressions like `1-3, 5, 8-10` with deduplication and bounds-checking.
- Automatic duplex discount calculation for paper savings.
- Client live-preview API backed by strictly enforced server calculations.

#### Module F: Demo Payment Processing
- Simulated payment options: Demo UPI (with generated simulated QR code) and Counter Cash.
- Strict state verification: prevents double payments or payments on cancelled orders.
- Explicit academic disclaimers: no financial details collected.

#### Module G: Simulated Printer Service
- Interface `PrinterService` implemented by `MockPrinterService`.
- Asynchronous lifecycle progression: `QUEUED` &rarr; `PRINTING` &rarr; `COMPLETED`.
- Hardware audit logs persisted in `KioskLogRepository`.

#### Module H: Order Tracking
- Public tracking page using secure 64-character token (`TRK-XXXX-XXXX`).
- 5-stage graphical progress timeline with live AJAX polling.
- Zero leakage of private document content on the tracking page.

#### Module I: Administrative Console
- Spring Security 6 session-based authentication with BCrypt password encoding.
- 6 KPI dashboard cards: Total Orders, Pending, Completed, Failed, Demo Sales, Active Kiosks.
- Search, multi-criteria filtering, pagination, order detail drilldown, retry, and cancellation.

#### Module J: File Privacy & Automated Cleanup
- Spring `@Scheduled` background worker running every 15 minutes.
- Purges physical files exceeding retention duration (`60 minutes`).
- Records cleanup events in audit log without logging document contents.

---

### 7. DATABASE DESIGN

#### Entities & Schema Relationships
1. **`Kiosk`**: `id`, `kiosk_identifier` (UNIQUE), `name`, `location`, `city`, `state`, `status`, `created_at`, `updated_at`.
2. **`PrintJob`**: `id`, `public_reference` (UNIQUE), `tracking_token` (UNIQUE), `kiosk_id`, `kiosk_name`, `original_filename`, `stored_filename`, `file_type`, `file_size_bytes`, `total_pages`, `selected_pages`, `selected_page_count`, `print_type`, `copies`, `duplex`, `unit_price`, `duplex_discount`, `total_amount`, `payment_status`, `job_status`, `ai_summary`, `ai_blank_pages_note`, `failure_reason`, `file_deleted`, `file_deleted_at`, `created_at`, `updated_at`, `completed_at`.
3. **`PaymentTransaction`**: `id`, `print_job_id` (FK to `PrintJob`), `transaction_reference` (UNIQUE), `payment_mode`, `amount`, `status`, `notes`, `created_at`, `updated_at`.
4. **`AdminUser`**: `id`, `username` (UNIQUE), `password_hash`, `full_name`, `role`, `enabled`, `created_at`.
5. **`KioskLog`**: `id`, `kiosk_id`, `order_reference`, `event_type`, `message`, `created_at`.

---

### 8. TESTING STRATEGY & ACTUAL RESULTS

Automated tests were written using JUnit 5, Mockito, and Spring Boot Test.

| Test Class | Test Case | Target Tested | Result |
|---|---|---|---|
| `PricingServiceTest` | `testBlackAndWhiteSingleCopyPrice` | ₹2.00 rate calculation | **PASSED** |
| `PricingServiceTest` | `testColourMultipleCopiesPrice` | ₹8.00 multi-copy calculation | **PASSED** |
| `PricingServiceTest` | `testDuplexPrintingDiscount` | Duplex paper sheet discount | **PASSED** |
| `PricingServiceTest` | `testZeroOrNegativeInputs` | Edge case bounds | **PASSED** |
| `PageRangeParserTest` | `testParseAllPages` | "ALL" keyword parsing | **PASSED** |
| `PageRangeParserTest` | `testParseComplexRange` | "1-3, 5, 8-10" syntax | **PASSED** |
| `PageRangeParserTest` | `testDeduplicateAndSort` | Deduplication of overlapping ranges | **PASSED** |
| `PageRangeParserTest` | `testStartGreaterThanEnd` | Rejection of "5-2" | **PASSED** |
| `PageRangeParserTest` | `testPageNumberExceedsTotal` | Out of bounds check | **PASSED** |
| `PageRangeParserTest` | `testNonNumericInput` | Letters in range rejection | **PASSED** |
| `PageRangeParserTest` | `testZeroOrNegativePageNumbers` | Zero/negative rejection | **PASSED** |
| `StorageServiceTest` | `testStoreValidPdf` | UUID randomization & save | **PASSED** |
| `StorageServiceTest` | `testRejectUnsupportedFormat` | Rejection of .exe/.sh | **PASSED** |
| `StorageServiceTest` | `testRejectExceededSize` | Rejection of >10MB | **PASSED** |
| `StorageServiceTest` | `testPathTraversalPrevention` | Rejection of `../` in names | **PASSED** |
| `DocumentAssistantServiceTest` | `testAnalyzePdfWithBlankPage` | Blank page warning & i18n | **PASSED** |
| `PaymentServiceTest` | `testProcessDemoPaymentSuccess` | Payment & printer trigger | **PASSED** |
| `PaymentServiceTest` | `testPreventDuplicatePayment` | Idempotency & duplicate check | **PASSED** |
| `SecurityAccessTest` | `testPublicHomePageAccess` | Public 200 OK | **PASSED** |
| `SecurityAccessTest` | `testPublicKioskAccess` | Public 200 OK | **PASSED** |
| `SecurityAccessTest` | `testPublicTrackSearchAccess` | Public 200 OK | **PASSED** |
| `SecurityAccessTest` | `testAdminProtectedWithoutAuth` | 302 redirect to login | **PASSED** |
| `SecurityAccessTest` | `testAdminAccessWithRole` | ROLE_ADMIN 200 OK | **PASSED** |

**Total Tests Executed:** 23  
**Failures:** 0 | **Errors:** 0 | **Skipped:** 0 | **Build Status:** SUCCESS

---

### 9. SECURITY AND PRIVACY
1. **Zero Customer Account Requirement:** Public users do not create accounts or leave permanent profiles.
2. **Encrypted Passwords:** Admin credentials hashed using BCrypt. Plaintext passwords are never stored.
3. **CSRF Protection:** Enabled on all sensitive forms; guest kiosk upload/payment endpoints explicitly managed.
4. **Path Traversal Protection:** Internal storage strictly strips all directory markers (`..`, `/`, `\`).
5. **Data Minimization:** Tracking pages display only status and order parameters—never document contents.
6. **Automatic Document Retention Purge:** Physical uploaded files are removed after the retention period expires.

---

### 10. LIMITATIONS
1. **Demonstration Mode:** Financial payments and printer hardware execution are simulated for academic evaluation.
2. **Physical Paper Handling:** Hardware issues such as paper jams, toner depletion, and physical paper replenishment are represented via simulated error states.
3. **OCR on Scanned Bitmaps:** While heuristic text density accurately identifies image-heavy pages, full OCR (optical character recognition) requires heavy native libraries (such as Tesseract) which were omitted to keep setup lightweight.

---

### 11. FUTURE ENHANCEMENTS
1. **Hardware Driver Adapter (CUPS / IPP / Java Print Service):** Connect the `PrinterService` interface to physical thermal receipt and network laser printers.
2. **Live Payment Gateway:** Replace `DemoPaymentServiceImpl` with Razorpay / PhonePe Indian payment gateways via Webhooks.
3. **Voice AI Assistance:** Add Web Speech API integration in Marathi and Hindi for hands-free voice guidance.
4. **Digital Locker (DigiLocker) Integration:** Allow citizens to pull verified government documents directly using Aadhaar OTP.

---

### 12. CONCLUSION
The **Print with AI** application successfully provides a reliable, secure, and user-friendly solution to the challenges of public document printing. By combining Spring Boot, Apache PDFBox, ZXing QR generation, trilingual localization (Marathi, Hindi, English), and robust security, the project delivers a presentation-ready prototype suitable for academic evaluation and real-world deployment.

---

### 13. REFERENCES
1. Spring Boot Documentation: https://spring.io/projects/spring-boot
2. Apache PDFBox: https://pdfbox.apache.org
3. ZXing Project: https://github.com/zxing/zxing
4. OWASP Top 10 Security Risks: https://owasp.org/www-project-top-ten/
5. Government of Maharashtra MahaOnline Citizen Services: https://aaplesarkar.mahaonline.gov.in
