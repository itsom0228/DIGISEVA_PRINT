# 🖨️ DigiSeva Print — Official Government & Institutional Self-Service Printing

> **Repository:** DIGISEVA_PRINT  
> **Brand Name:** DigiSeva Print  
> **Tagline:** Official Digital Document & Printing Seva  
> **Multilingual Support:** English (EN) | मराठी (MR) | हिंदी (HI)

---

## 📖 1. Project Overview

**DigiSeva Print** is a full-stack, enterprise-grade self-service printing and institutional document platform engineered for government offices, revenue tahsil centers, municipal corporations, gram panchayats, civil hospitals, and universities.

### Real-World Production Architecture
1. **Physical Machine Hardware Binding:** Each machine is deployed in a dedicated office (e.g., Tahsil Office, College, Hospital) with locked location security. Customers cannot change the printing location from the user interface.
2. **Strict Office Data Isolation:** Each office displays **only its authorized application forms** (e.g., Income Certificates for Tahsil, Bonafide for College). Forms and data from other institutions are strictly isolated and never mixed.
3. **Admin Central Management:** Only administrators have access to configure machines, register new hardware, assign office categories, and adjust operational parameters.
4. **Dedicated Kiosk Display Terminal:** Physical laptops and kiosk screens display an ATM-grade full-screen terminal interface (`/kiosk/display`), removed from public customer navigation.
5. **Streamlined Citizen Portal:** Clean homepage focused exclusively on **Start Printing Now**, **Track Existing Order**, and **Applications of the Selected Office**.

---

## 🛠️ 2. Technology Stack

- **Backend:** Java 17+, Spring Boot 3.4.x, Spring MVC, Spring Data JPA, Hibernate, Spring Security 6.
- **Frontend:** Thymeleaf template engine, HTML5, CSS3, JavaScript, Bootstrap 5, Bootstrap Icons.
- **Document Intelligence:** Apache PDFBox 3.0.4 (page counting, text density analysis, blank page heuristics) + optional local Ollama integration.
- **QR Code Generation:** ZXing (Zebra Crossing) 3.5.3 (high-resolution PNG matrix generation & download).
- **Security:** Spring Security session-based auth, BCrypt password hashing, role-based authorization (`ROLE_ADMIN`), path traversal safeguards, randomized storage tokens.
- **Database:**
  - **H2 In-Memory/File:** Default demonstration and testing profile (`demo`). Zero external installation required.
  - **MySQL 8:** Production profile (`mysql`) with configurable environment variables.
- **Testing:** JUnit 5, Mockito, Spring Boot Test, Spring Security Test (23 automated test cases).

---

## 🌐 3. Trilingual Multilingual Support (English, मराठी, हिंदी)

To ensure usability for first-time smartphone users and rural citizens across Maharashtra and India, the entire UI and AI assistant recommendations are available in three languages:
- **English (EN)**
- **मराठी (MR)**
- **हिंदी (HI)**

Language can be switched instantly via the dropdown in the navigation bar or by appending `?lang=en`, `?lang=mr`, or `?lang=hi` to any URL.

---

## 🚀 4. How to Run Locally (Windows Quickstart)

### Prerequisites
- Java JDK 17 or higher (Java 25 is supported).
- Apache Maven (A pre-configured Maven 3.9.6 distribution and convenience wrappers `mvn.cmd` are included in the repository).

### Step 1: Clone / Open Project
Open terminal or PowerShell in the root directory:
```powershell
cd "O:\Print With AI"
```

### Step 2: Run Automated Tests
Execute all unit and integration tests:
```powershell
.\mvn.cmd clean test
```
*(All 23 unit & integration tests pass with 0 failures and 0 errors).*

### Step 3: Run the Application (Default H2 Demo Profile)
Start the Spring Boot server:
```powershell
.\mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```
Or from inside `print-with-ai/`:
```powershell
cd print-with-ai
..\mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

The application will start at:
👉 **[http://localhost:8085](http://localhost:8085)**  
*(Port 8085 is configured by default to avoid conflicts with background Tomcat servers).*

---

## 📱 5. Step-by-Step Faculty Demonstration Flow

| Step | Action | URL / Screen | Key Highlight |
|---|---|---|---|
| **1** | Open Home Page | `http://localhost:8085/` | View modern SaaS hero, 30-sec product explanation, and trilingual language switcher. |
| **2** | Switch Language | Click Language &rarr; **मराठी** or **हिंदी** | Shows complete Marathi/Hindi localized interface. |
| **3** | View Kiosk & QR | `http://localhost:8085/kiosk/TAHSIL001` | Displays generated QR code for Tahsil Demonstration Kiosk and download button. |
| **4** | Upload Document | `http://localhost:8085/print/new?kioskId=TAHSIL001` | Drag & drop `samples/Tahsil_Farmer_Application.pdf`. |
| **5** | Smart AI Assistant | `http://localhost:8085/print/configure/{token}` | **AI flags Page 3** as having low text density (affidavit seal page) and suggests double-sided printing. |
| **6** | Configure Options | Change copies, select B&W or Colour | Live dynamic price summary automatically recalculates based on server rates. |
| **7** | Demo Payment | `http://localhost:8085/print/payment/{token}` | Select **Demo UPI (Simulated QR)** or **Demo Cash**; click **Confirm Demo Payment**. |
| **8** | Simulated Printing | `http://localhost:8085/print/confirmation/{token}` | Watch live progress bar transition: `QUEUED` &rarr; `PRINTING` &rarr; `COMPLETED`. |
| **9** | Order Tracking | `http://localhost:8085/order/track/{token}` | View 5-stage lifecycle timeline using secure tracking token without exposing private data. |
| **10** | Admin Dashboard | `http://localhost:8085/admin/login` | Login with `admin` / `admin123`. View 6 KPI cards, audit logs, order inspection, and retry/cancel actions. |

---

## 🔑 6. Default Demonstration Credentials

- **Username:** `admin`
- **Password:** `admin123`
- **Role:** `ROLE_ADMIN`
- **Console URL:** [http://localhost:8085/admin/login](http://localhost:8085/admin/login)

---

## 🗄️ 7. Database Configuration

### A. Default H2 In-Memory Database (No Setup Required)
- Spring Profile: `demo`
- H2 Web Console: [http://localhost:8085/h2-console](http://localhost:8085/h2-console)
- JDBC URL: `jdbc:h2:mem:printwithai_db`
- Username: `sa`
- Password: *(leave blank)*

### B. MySQL 8 Database (Optional Production Profile)
1. Ensure MySQL 8 is running on port 3306.
2. Execute the script: `print-with-ai/src/main/resources/mysql-schema.sql`.
3. Set environment variables or update `.env`:
   ```bash
   SPRING_PROFILES_ACTIVE=mysql
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=printwithai_db
   DB_USERNAME=root
   DB_PASSWORD=your_password
   ```
4. Run:
   ```powershell
   .\mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
   ```

---

## 💰 8. Pricing Configuration

Demonstration rates are configurable in `application.properties` without touching Java code:
```properties
app.pricing.bw-per-page=2.00        # ₹2.00 per Black & White page per copy
app.pricing.color-per-page=8.00     # ₹8.00 per Colour page per copy
app.pricing.duplex-discount=0.50    # ₹0.50 discount per back sheet saved in duplex mode
```
Calculation Formula:
$$\text{Total Price} = (\text{Selected Pages} \times \text{Copies} \times \text{Unit Rate}) - \text{Duplex Discount}$$

---

## 🤖 9. AI Document Assistant Details

- **Heuristic Intelligence Engine (Default):**
  - Page-by-page text extraction via Apache PDFBox.
  - Text density and word count calculation.
  - Identification of pages with $< 25$ characters / $< 5$ words (potential blank pages or scanned photo receipts).
  - Trilingual advisory messages in Marathi, Hindi, and English.
- **Optional Local Neural AI (Ollama):**
  - Toggleable via `app.ai.ollama.enabled=true`.
  - Connects to local endpoint `http://localhost:11434/api/generate` (e.g., Llama 3.2 or Mistral).
  - Graceful fallback: If Ollama is offline or times out (3000ms), the application automatically falls back to the heuristic engine without throwing errors.

---

## 🛡️ 10. Document Privacy & Retention Policy

1. **Storage Isolation:** Uploaded documents are stored outside the public static directory in `./uploads/`.
2. **UUID Obfuscation:** Files are given randomized internal UUID names (e.g., `4a59-8f...pdf`) to prevent path traversal and unauthorized direct guessing.
3. **Scheduled Cleanup:** A background scheduler runs every 15 minutes (`FileCleanupService`) and permanently purges uploaded files exceeding the retention window (`app.storage.retention-minutes=60`).
4. **Token Security:** Orders are accessed only via random 64-character tracking tokens (`TRK-XXXX-XXXX`), preventing sequential ID harvesting.

---

## 📁 11. Project Structure

```
o:\Print With AI\
├── .env.example                     # Environment variables template
├── .gitignore                       # Git ignore rules
├── mvn.cmd                          # Root Maven convenience runner
├── pom.xml                          # Aggregator POM
├── README.md                        # Project documentation
├── samples/                         # Real sample PDFs for testing
│   ├── Tahsil_Farmer_Application.pdf
│   └── Student_Project_Report.pdf
├── docs/
│   ├── PROJECT_REPORT.md            # Complete academic project report
│   ├── PRESENTATION_OUTLINE.md      # 10-slide viva presentation deck
│   ├── DEMO_SCRIPT.md               # Step-by-step oral demonstration script
│   └── VIVA_QUESTIONS.md            # Examiner viva Q&A
└── print-with-ai/                   # Spring Boot Application Module
    ├── pom.xml                      # Maven dependencies and build
    └── src/
        ├── main/
        │   ├── java/com/printwithai/
        │   │   ├── PrintWithAiApplication.java
        │   │   ├── ai/              # DocumentAssistantService
        │   │   ├── config/          # WebMvcConfig (i18n), SecurityConfig
        │   │   ├── controller/      # Home, Order, Payment, Kiosk, Admin, Api
        │   │   ├── dto/             # Request & Response DTOs
        │   │   ├── entity/          # Kiosk, PrintJob, PaymentTransaction, AdminUser, KioskLog
        │   │   ├── enums/           # PrintType, PaymentMode, PaymentStatus, PrintJobStatus
        │   │   ├── exception/       # GlobalExceptionHandler, Custom exceptions
        │   │   ├── printer/         # PrinterService, MockPrinterService
        │   │   ├── repository/      # Spring Data JPA Repositories
        │   │   ├── scheduler/       # FileCleanupService
        │   │   ├── security/        # CustomUserDetailsService, SecurityConfig
        │   │   ├── service/         # PrintJobService, KioskService, PricingService, QrCodeService
        │   │   ├── storage/         # StorageService (UUID storage & validation)
        │   │   └── util/            # PageRangeParser, ReferenceGenerator
        │   └── resources/
        │       ├── application.properties
        │       ├── application-mysql.properties
        │       ├── messages.properties       # English i18n
        │       ├── messages_mr.properties    # Marathi i18n
        │       ├── messages_hi.properties    # Hindi i18n
        │       ├── schema.sql, mysql-schema.sql
        │       ├── static/css/style.css
        │       └── templates/                # Thymeleaf templates (admin, kiosk, order, error)
        └── test/
            └── java/com/printwithai/        # 23 Unit and Integration Tests
```

---

## 👥 Authors & Academic Credits
- **Project Name:** Print with AI (AI-Assisted Smart Printing and Document Kiosk)
- **Academic Submission:** Final Year Engineering Capstone Project
- **Discipline:** Computer Science & Information Technology
