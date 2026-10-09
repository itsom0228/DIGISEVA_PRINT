# 🎬 STEP-BY-STEP VIVA DEMO SCRIPT
## Project: PRINT WITH AI — Live Demonstration Guide

This script guides the presenter through the exact sequence of actions and spoken explanations during the viva presentation.

---

### PRE-DEMO PREPARATION (2 Minutes Before Faculty Arrives)
1. Open PowerShell in `O:\Print With AI\print-with-ai`.
2. Start the application:
   ```powershell
   ..\mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
   ```
3. Verify output ends with:
   `Tomcat started on port 8085 (http) with context path '/'`
   `Started PrintWithAiApplication in ... seconds`
4. Open your web browser to: **`http://localhost:8085`**
5. Have the sample document ready in folder: `O:\Print With AI\samples\Tahsil_Farmer_Application.pdf`

---

### ACT 1: INTRODUCTION & LANDING PAGE (1 Minute)
- **Action:** Point your browser to `http://localhost:8085`.
- **What to show:**
  - The clean SaaS navbar with logo **PRINT WITH AI**, taglines, and call-to-action buttons.
  - The hero card featuring the Tahsil Office demonstration kiosk with its live ZXing-generated QR code.
  - The "How It Works in 4 Steps" section and key feature cards.
- **What to say:**
  > "Good morning, respected professors. Our project is 'Print with AI'—an automated document printing kiosk designed for public government offices, colleges, and libraries. To prevent citizens from having to send their sensitive files to strangers over WhatsApp, our platform allows any citizen with a smartphone to simply scan the kiosk QR code and submit their print job directly."

---

### ACT 2: MULTILINGUAL SUPPORT (MARATHI & HINDI) (1 Minute)
- **Action:**
  - Click the **Language** dropdown in the top navbar.
  - Select **मराठी (MR)**.
  - Scroll down to show that headlines, descriptions, button labels, and step guides are now translated into clear Marathi.
  - Switch to **हिंदी (HI)** and then back to **English** or Marathi.
- **What to say:**
  > "Because this is designed for public spaces like Tahsil and Talathi offices in Maharashtra, accessibility for Marathi and Hindi speakers is essential. We implemented a complete internationalization system using Spring MessageSource and cookie-backed locale resolvers so users can read instructions in their mother tongue."

---

### ACT 3: KIOSK ACCESS & QR CODE DOWNLOAD (1 Minute)
- **Action:**
  - Click on **Kiosks & QR** in the top navigation (`/kiosk`).
  - Click **Full QR & Details** on the **Tahsil Office Demo Kiosk** (`TAHSIL001`).
  - Point out the large QR image, the target URL (`http://localhost:8085/print/new?kioskId=TAHSIL001`), and click **Download Printable QR**.
- **What to say:**
  > "Each physical kiosk in our network has a unique identifier such as 'TAHSIL001'. The QR code is generated dynamically on our server using the ZXing library. When a citizen scans this QR with their phone camera, it immediately launches the upload screen bound to this specific kiosk."

---

### ACT 4: UPLOAD & SMART AI DOCUMENT ASSISTANT (2 Minutes)
- **Action:**
  - Click **Start Printing Now** or open `http://localhost:8085/print/new?kioskId=TAHSIL001`.
  - Drag and drop `samples/Tahsil_Farmer_Application.pdf` into the upload dropzone (or click Browse Files).
  - Click **Analyze & Configure Print Options**.
  - Show the **Your Smart Document Assistant** panel!
- **What to point out:**
  1. The AI engine extracted the document text using Apache PDFBox.
  2. Total page count: **3 Pages**.
  3. Notice the yellow warning badge: **"Notice: Some pages have low text density. Pages [3] contain very little text."**
- **What to say:**
  > "Here is our AI Document Assistant. Using Apache PDFBox, our server parsed the PDF without requiring an external paid API. It detected that the document has 3 pages, but Page 3 has very few characters. In our test document, Page 3 is an affidavit endorsement seal page. In traditional shops, people often print blank or unintended pages without knowing. Our AI flags this immediately so the user can verify before paying."

---

### ACT 5: CONFIGURATION & DYNAMIC PRICING (1.5 Minutes)
- **Action:**
  - Point to the **Order Summary (Live Preview)** card on the right.
  - Change Print Mode from **Black & White** (₹2.00) to **Colour** (₹8.00). Watch the total price update instantly.
  - Change number of copies to `2`. Watch the price update to ₹48.00.
  - Switch back to **Black & White** and toggle **Double-Sided Printing (Duplex)**.
  - Point out the green line: **Duplex Savings applied (-₹1.00)**.
- **What to say:**
  > "All pricing calculations are performed using Java BigDecimal with demonstration rates configured in application.properties. When the user enables Duplex printing, our system calculates physical sheet savings and automatically applies a paper discount."

---

### ACT 6: DEMO PAYMENT SIMULATION (1 Minute)
- **Action:**
  - Click **Proceed to Demo Payment**.
  - Highlight the yellow disclaimer: **DEMO MODE: No real payment is being processed.**
  - Show the simulated UPI QR code and simulated UPI ID handle (`student@okhdfcbank`).
  - Click **Confirm Demo Payment**.
- **What to say:**
  > "To ensure complete presentation safety without requiring real debit cards or financial credentials, we implemented a simulated payment strategy. It records a unique transaction reference and updates the order status to DEMO_SUCCESS, triggering the printer service."

---

### ACT 7: SIMULATED PRINTER LIFECYCLE & LIVE TRACKING (1.5 Minutes)
- **Action:**
  - Watch the confirmation page!
  - Point to the live progress bar:
    1. First `QUEUED` (40%)
    2. Then `PRINTING` (75%)
    3. Finally `COMPLETED` (100% Green).
  - Click **Open Order Tracking Page**.
  - Show the 5-stage graphical timeline.
- **What to say:**
  > "Here, our MockPrinterService asynchronously advances the job through its hardware spooler states—from QUEUED to PRINTING to COMPLETED. The client page polls our REST endpoint and updates the status in real time. The citizen can track this anytime using their secure tracking token."

---

### ACT 8: ADMIN DASHBOARD & SECURITY (2 Minutes)
- **Action:**
  - Click **Admin Portal** or navigate to `http://localhost:8085/admin/login`.
  - Enter credentials:
    - Username: `admin`
    - Password: `admin123`
  - Click **Sign In to Dashboard**.
- **What to show:**
  1. The 6 KPI metric cards: Total Orders, Pending, Completed, Failed, Demo Sales, and Active Kiosks.
  2. The orders table showing the order you just processed (`PAI-2026-XXXX`).
  3. Click the eye icon to view **Order Details**.
  4. Point out the internal stored UUID filename (e.g. `83fb4a...pdf`) versus the original filename, proving path-traversal and file isolation.
  5. Show the audit logs and the **Purge Expired Files** button.
- **What to say:**
  > "The admin dashboard is protected by Spring Security with session-based authentication and BCrypt password encryption. Administrators have full visibility into orders, hardware audit logs, and can trigger emergency retries or cancellations. Finally, our scheduled FileCleanupService automatically purges files after 60 minutes to maintain complete citizen privacy."

---

### SUMMARY CLOSING
> "That concludes our demonstration. The system is functional from start to finish, thoroughly tested, and ready for your viva questions."
