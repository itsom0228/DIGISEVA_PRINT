# 🎓 VIVA VOCE QUESTIONS & MODEL ANSWERS
## Project: PRINT WITH AI — AI-Assisted Smart Printing and Document Kiosk

This guide contains the most likely questions asked by academic examiners and project evaluation panels, together with technically sound, concise answers.

---

### CATEGORY 1: ARCHITECTURE & TECHNOLOGY SELECTION

#### Q1: Why did you choose Java and Spring Boot instead of Node.js or Python Django?
**Answer:**
> "Spring Boot provides enterprise-grade robustness, thread-safe memory management, compile-time type safety, and seamless integration with industry-standard libraries such as Apache PDFBox, Spring Security, and Hibernate ORM. For hardware kiosks and financial-adjacent workflows, Java’s strong typing, transaction management (`@Transactional`), and mature security ecosystem prevent common runtime errors and scale reliably across concurrent public users."

#### Q2: Explain your application’s layered architecture.
**Answer:**
> "We followed a strict 4-layer architecture:
> 1. **Presentation Layer:** Thymeleaf views and REST controllers (`PrintOrderController`, `ApiController`).
> 2. **Service Layer:** Business logic, pricing formulas, document intelligence, and printer lifecycle (`PrintJobService`, `PricingService`, `DocumentAssistantService`).
> 3. **Data Access Layer:** Spring Data JPA repositories (`PrintJobRepository`, `KioskRepository`).
> 4. **Persistence Layer:** Relational databases using Hibernate 6 (H2 for zero-config demonstration and MySQL 8 for production)."

#### Q3: Why did you avoid Tailwind CSS and choose Bootstrap 5 with custom CSS?
**Answer:**
> "Bootstrap 5 delivers a rock-solid responsive grid system and accessible UI components without requiring a Node.js build pipeline or complex npm tooling. We enriched it with custom CSS implementing modern SaaS aesthetics—such as glassmorphism cards, restrained shadows, custom gradients, and trilingual typography—ensuring the application looks premium while running cleanly as a self-contained Spring Boot project."

---

### CATEGORY 2: QR CODE & KIOSK INTERACTION

#### Q4: How does the QR-based kiosk access work? Does it require an internet connection or an external QR generator?
**Answer:**
> "The QR code is generated entirely locally on the server using Google's **ZXing (Zebra Crossing)** library. When the kiosk endpoint `/kiosk/qr/{kioskId}` is called, the ZXing `QRCodeWriter` generates a 2D bit matrix encoding the URL: `http://<host>:<port>/print/new?kioskId=TAHSIL001`. No external third-party QR generation service is used, guaranteeing high security and zero external dependencies."

#### Q5: How do you associate a user's print job with the correct physical kiosk?
**Answer:**
> "When the citizen scans the QR code, the `kioskId` parameter is passed in the URL. On submission, the backend validates that the kiosk exists in the database. The resulting `PrintJob` entity stores the `kioskId` as a foreign reference, ensuring that the printer dispatch only routes the job to the designated kiosk spooler."

---

### CATEGORY 3: DOCUMENT INTELLIGENCE & AI

#### Q6: Where is AI used in this project? Did you just use the word 'AI' as a buzzword?
**Answer:**
> "No, we implemented genuine document intelligence in `DocumentAssistantService`. 
> 1. **Heuristic Document Intelligence (Default):** Using Apache PDFBox 3.x, the system parses the PDF document structure page-by-page, extracts text streams using `PDFTextStripper`, calculates text density and word count, and flags pages with fewer than 25 characters or 5 words. This detects potential blank pages or scanned photo pages before printing, saving paper and user money.
> 2. **Local Neural AI (Optional):** We provide an architectural adapter for a locally hosted **Ollama** LLM (e.g., Llama 3.2). When enabled via configuration, it sends text extracts to generate concise summaries and guidance. If Ollama is offline, the service falls back gracefully to the heuristic engine without failing."

#### Q7: Can low text density definitely prove that a page is blank?
**Answer:**
> "No, and we explicitly account for that. A scanned document or a page containing only stamps, signatures, or images also has low or zero extractable text streams. Therefore, our UI describes these detections as *helpful suggestions* rather than guaranteed detections: *'Notice: Some pages have low text density. They may be blank or scanned photos. Please review before printing.'*"

---

### CATEGORY 4: PRICING & DATA ACCURACY

#### Q8: Why did you use `BigDecimal` instead of `double` or `float` for prices?
**Answer:**
> "`float` and `double` use binary floating-point representation (IEEE 754), which causes precision loss and rounding errors (for instance, `0.1 + 0.2 = 0.30000000000000004`). In financial and billing systems, this is unacceptable. `BigDecimal` allows exact arbitrary-precision arithmetic with controlled rounding modes (`RoundingMode.HALF_UP`), ensuring 100% accurate currency calculations."

#### Q9: Can a user manipulate the price by tampering with frontend JavaScript?
**Answer:**
> "No. While the frontend calls `/api/price-estimate` to render a dynamic preview for user convenience, the server completely ignores any price submitted by the browser. When the order configuration form is submitted, `PrintJobService` independently recalculates the total amount on the server using verified page counts and rates stored in `PricingService`."

---

### CATEGORY 5: DEMO PAYMENT & PRINTING SIMULATION

#### Q10: How does the demo payment differ from a live payment gateway?
**Answer:**
> "Our demo payment simulator records a mock `PaymentTransaction` with status `DEMO_SUCCESS` and a generated transaction reference (`TXN-DEMO-XXXX`). It displays clear disclaimers that no real money is processed and never asks for debit card numbers or UPI PINs. However, the service follows a standard Strategy design pattern (`PaymentService`), so an official Razorpay or PhonePe webhook adapter can be swapped in without modifying any order management logic."

#### Q11: How is the physical printing simulated?
**Answer:**
> "The `MockPrinterService` implements the `PrinterService` interface. When an order is paid, it submits the job and uses an asynchronous execution worker (`CompletableFuture.runAsync`). It pauses for 2 seconds in `QUEUED`, transitions to `PRINTING`, simulates paper feed duration for 3 seconds, and updates the database to `COMPLETED`. The tracking screen polls `/order/api/status/{token}` every 1.5 seconds, displaying real-time hardware status changes."

---

### CATEGORY 6: SECURITY & CITIZEN PRIVACY

#### Q12: How are uploaded documents protected from unauthorized access and path traversal?
**Answer:**
> "We implement four layers of protection:
> 1. **Storage Location:** Files are stored in `./uploads/`, completely outside the public web root (`/static`).
> 2. **Filename Randomization:** The original filename is recorded in the database, but the physical file is renamed to a random `UUID.randomUUID().toString() + .pdf`.
> 3. **Path Traversal Sanitization:** The filename is cleaned using `StringUtils.cleanPath()` and rejected if it contains `..` or slash separators.
> 4. **Randomized Tracking Tokens:** Orders cannot be accessed using sequential database IDs (like `/order/1`). Instead, they require a 64-character token (`TRK-XXXX-XXXX`)."

#### Q13: What happens to files after printing? How is retention handled?
**Answer:**
> "We implemented an automated privacy purge via `FileCleanupService`. A Spring `@Scheduled(fixedRate = 900000)` task runs every 15 minutes, queries all jobs older than the configurable retention window (default 60 minutes), and permanently deletes the files from disk. The database record is updated to `fileDeleted = true` with a timestamp."

---

### CATEGORY 7: INTERNATIONALIZATION (i18n)

#### Q14: How did you implement Marathi, Hindi, and English multilingual support?
**Answer:**
> "We utilized Spring MVC's internationalization framework:
> - `ResourceBundleMessageSource` reads `messages.properties` (English), `messages_mr.properties` (Marathi), and `messages_hi.properties` (Hindi).
> - `CookieLocaleResolver` persists the user's selected language in an HTTP cookie (`PAI_LOCALE`).
> - `LocaleChangeInterceptor` detects the `?lang=en|mr|hi` query parameter on any request.
> - Thymeleaf renders strings dynamically using `#{key}` expressions, including server-side AI recommendation messages."

---

### CATEGORY 8: TESTING & LIMITATIONS

#### Q15: What testing did you perform on the application?
**Answer:**
> "We implemented 23 automated tests using JUnit 5 and Mockito:
> - Pricing calculations (B&W, Colour, Multi-copy, Duplex discount).
> - Page range expression parser (valid ranges, bounds checking, negative inputs, invalid letters).
> - Storage service security (valid upload, rejection of `.exe`, file size limits, path traversal).
> - Document assistant heuristics and blank page detection.
> - Payment idempotency and state transitions.
> - Spring Security role-based access to admin routes.
> All 23 tests execute and pass with 0 failures."

#### Q16: How can this prototype be deployed on real hardware in the future?
**Answer:**
> "To transition to production:
> 1. Connect physical printers using Java Print Service (`javax.print`) or a CUPS network daemon on a Raspberry Pi kiosk terminal.
> 2. Plug in an official payment gateway (Razorpay UPI QR) implementing `PaymentService`.
> 3. Package the Spring Boot application as an executable JAR or Docker container running on a secure Linux/Windows kiosk machine."
