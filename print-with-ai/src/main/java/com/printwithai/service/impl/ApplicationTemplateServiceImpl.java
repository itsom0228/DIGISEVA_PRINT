package com.printwithai.service.impl;

import com.printwithai.dto.AdminTemplateFormDto;
import com.printwithai.dto.TemplateSearchFilterDto;
import com.printwithai.entity.ApplicationTemplate;
import com.printwithai.entity.OfficeCategory;
import com.printwithai.entity.TemplateDownload;
import com.printwithai.exception.ResourceNotFoundException;
import com.printwithai.repository.ApplicationTemplateRepository;
import com.printwithai.repository.OfficeCategoryRepository;
import com.printwithai.repository.TemplateDownloadRepository;
import com.printwithai.service.ApplicationTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationTemplateServiceImpl implements ApplicationTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationTemplateServiceImpl.class);

    private final OfficeCategoryRepository categoryRepository;
    private final ApplicationTemplateRepository templateRepository;
    private final TemplateDownloadRepository downloadRepository;

    public ApplicationTemplateServiceImpl(OfficeCategoryRepository categoryRepository,
                                         ApplicationTemplateRepository templateRepository,
                                         TemplateDownloadRepository downloadRepository) {
        this.categoryRepository = categoryRepository;
        this.templateRepository = templateRepository;
        this.downloadRepository = downloadRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficeCategory> getAllActiveCategoriesWithCounts() {
        List<OfficeCategory> categories = categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc();
        for (OfficeCategory cat : categories) {
            cat.setTemplateCount(templateRepository.countByCategoryAndActiveTrue(cat));
        }
        return categories;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficeCategory> getAllCategories() {
        List<OfficeCategory> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        for (OfficeCategory cat : categories) {
            cat.setTemplateCount(templateRepository.countByCategoryAndActiveTrue(cat));
        }
        return categories;
    }

    @Override
    @Transactional(readOnly = true)
    public OfficeCategory getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Office category not found with slug: " + slug));
    }

    @Override
    @Transactional(readOnly = true)
    public OfficeCategory getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Office category not found with ID: " + id));
    }

    @Override
    @Transactional
    public OfficeCategory saveCategory(OfficeCategory category) {
        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationTemplate> searchTemplates(TemplateSearchFilterDto filter) {
        Sort sort = Sort.by(Sort.Direction.DESC, "downloadCount");
        if ("recent".equalsIgnoreCase(filter.getSort())) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        } else if ("title".equalsIgnoreCase(filter.getSort())) {
            sort = Sort.by(Sort.Direction.ASC, "title");
        }

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        return templateRepository.searchTemplates(
                filter.getKeyword(),
                filter.getCategory(),
                filter.getLanguage(),
                filter.getFileType(),
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationTemplate> getTemplatesByCategory(String categorySlug, int page, int size) {
        OfficeCategory category = getCategoryBySlug(categorySlug);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "downloadCount"));
        return templateRepository.findByCategoryAndActiveTrue(category, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationTemplate getTemplateBySlug(String slug) {
        return templateRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Application template not found with slug: " + slug));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application template not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationTemplate> getFeaturedTemplates() {
        return templateRepository.findTop8ByActiveTrueOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationTemplate> getPopularTemplates() {
        return templateRepository.findTop6ByActiveTrueOrderByDownloadCountDesc();
    }

    @Override
    @Transactional
    public ApplicationTemplate createTemplate(AdminTemplateFormDto form) {
        OfficeCategory category = getCategoryById(form.getCategoryId());

        ApplicationTemplate template = new ApplicationTemplate();
        template.setTitle(form.getTitle());
        template.setSlug(form.getSlug());
        template.setDescription(form.getDescription());
        template.setPurpose(form.getPurpose());
        template.setLanguage(form.getLanguage());
        template.setCategory(category);
        template.setTemplateContent(form.getTemplateContent());
        template.setOfficialTemplate(form.isOfficialTemplate());
        template.setVerified(form.isVerified());
        template.setSourceName(form.getSourceName());
        template.setSourceUrl(form.getSourceUrl());
        template.setInstructions(form.getInstructions());
        template.setRequiredDocuments(form.getRequiredDocuments());
        template.setActive(form.isActive());
        template.setFileType("TEXT_TEMPLATE");

        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public ApplicationTemplate updateTemplate(Long id, AdminTemplateFormDto form) {
        ApplicationTemplate template = getTemplateById(id);
        OfficeCategory category = getCategoryById(form.getCategoryId());

        template.setTitle(form.getTitle());
        template.setSlug(form.getSlug());
        template.setDescription(form.getDescription());
        template.setPurpose(form.getPurpose());
        template.setLanguage(form.getLanguage());
        template.setCategory(category);
        template.setTemplateContent(form.getTemplateContent());
        template.setOfficialTemplate(form.isOfficialTemplate());
        template.setVerified(form.isVerified());
        template.setSourceName(form.getSourceName());
        template.setSourceUrl(form.getSourceUrl());
        template.setInstructions(form.getInstructions());
        template.setRequiredDocuments(form.getRequiredDocuments());
        template.setActive(form.isActive());

        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public void toggleTemplateActive(Long id) {
        ApplicationTemplate template = getTemplateById(id);
        template.setActive(!template.isActive());
        templateRepository.save(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        ApplicationTemplate template = getTemplateById(id);
        templateRepository.delete(template);
    }

    @Override
    @Transactional
    public void recordTemplateDownload(Long templateId, String actionType, String kioskId) {
        try {
            ApplicationTemplate template = getTemplateById(templateId);
            templateRepository.incrementDownloadCount(templateId);
            downloadRepository.save(new TemplateDownload(template, actionType, kioskId));
        } catch (Exception e) {
            log.warn("Failed to log template download event for template ID {}: {}", templateId, e.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    @Override
    public void seedInitialCategoriesAndTemplates() {
        if (categoryRepository.count() > 0) {
            return; // Already initialized
        }
        log.info("Seeding initial Office Categories and Application Templates Catalogue...");

        // 1. College & University
        OfficeCategory college = categoryRepository.save(new OfficeCategory(
                "College & University", "college-university",
                "Certificates, NOCs, Bonafide and academic requests for college students.",
                "bi-mortarboard-fill", 1));

        // 2. Tahsil Office
        OfficeCategory tahsil = categoryRepository.save(new OfficeCategory(
                "Tahsil Office", "tahsil-office",
                "Revenue applications, caste certificates, income verification and land record requests.",
                "bi-building-fill-check", 2));

        // 3. Gram Panchayat
        OfficeCategory panchayat = categoryRepository.save(new OfficeCategory(
                "Gram Panchayat", "gram-panchayat",
                "Rural village administration, residence proof, birth and death verification applications.",
                "bi-houses-fill", 3));

        // 4. Municipal Corporation
        OfficeCategory municipal = categoryRepository.save(new OfficeCategory(
                "Municipal Corporation", "municipal-corporation",
                "Urban civic amenities, property tax records, birth/death extract requests and public grievances.",
                "bi-bank2", 4));

        // 5. Government Offices
        OfficeCategory govt = categoryRepository.save(new OfficeCategory(
                "Government Offices", "government-offices",
                "General administrative representations, RTI letters and public service requests.",
                "bi-shield-shaded", 5));

        // 6. Hospital
        OfficeCategory hospital = categoryRepository.save(new OfficeCategory(
                "Hospital", "hospital",
                "Medical certificate requests, medical records extracts, duplicate fitness reports and administration.",
                "bi-hospital-fill", 6));

        // 7. Bank
        OfficeCategory bank = categoryRepository.save(new OfficeCategory(
                "Bank", "bank",
                "Formal customer requests for bank statements, address updates, name corrections and account closure.",
                "bi-bank", 7));

        // 8. Police Station
        OfficeCategory police = categoryRepository.save(new OfficeCategory(
                "Police Station", "police-station",
                "Formal report submissions, lost document declarations, and general non-cognizable intimations.",
                "bi-shield-check", 8));

        // 9. Court
        OfficeCategory court = categoryRepository.save(new OfficeCategory(
                "Court", "court",
                "Certified copies request, vakalatnama inspection applications, and record verification requests.",
                "bi-award-fill", 9));

        // 10. Other Institutions
        OfficeCategory other = categoryRepository.save(new OfficeCategory(
                "Other Institutions", "other-institutions",
                "Hostel admissions, library membership, utility connections, and institutional service requests.",
                "bi-folder-fill", 10));

        // ==========================================
        // SEED TEMPLATES
        // ==========================================

        // --- COLLEGE & UNIVERSITY ---
        seedTemplate(new ApplicationTemplate(
                "Bonafide Certificate Application",
                "bonafide-certificate-application",
                "Standard application to the college Principal for issuing a Bonafide Certificate for scholarship or bus pass.",
                "Request a Bonafide Certificate from College/University",
                "en", college,
                "To,\n" +
                "The Principal,\n" +
                "[College Name],\n" +
                "[Address]\n\n" +
                "Date: [Date]\n\n" +
                "Subject: Application for issuance of Bonafide Certificate\n\n" +
                "Respected Sir/Madam,\n\n" +
                "I am [Applicant Name], a bonafide student of your esteemed institution currently studying in [Address / Department] with Roll Number / PRN: [Roll Number].\n\n" +
                "I humbly request you to issue a Bonafide Certificate in my name for the purpose of [Purpose]. I have attached a copy of my current student ID card and college fee receipt for your kind verification.\n\n" +
                "Kindly grant me the certificate at the earliest convenience.\n\n" +
                "Thanking you.\n\n" +
                "Yours obediently,\n" +
                "[Applicant Name]\n" +
                "Roll No: [Roll Number]\n" +
                "Contact: [Contact Number]",
                false, true
        ), "ID Card Copy, Previous Year Marksheet, Fee Receipt", "Submit to the Student Section / Academic Clerk with fee receipt.");

        seedTemplate(new ApplicationTemplate(
                "बोनाफाईड प्रमाणपत्र मागणी अर्ज (Bonafide Certificate)",
                "bonafide-certificate-marathi",
                "महाविद्यालयातील प्राचार्यांकडे बोनाफाईड प्रमाणपत्रासाठी सादर करावयाचा अधिकृत नमुना अर्ज.",
                "महाविद्यालयातून बोनाफाईड प्रमाणपत्र मिळण्याबाबत",
                "mr", college,
                "प्रति,\n" +
                "मा. प्राचार्य महोदय,\n" +
                "[College Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: बोनाफाईड प्रमाणपत्र (Bonafide Certificate) मिळण्याबाबत अर्ज.\n\n" +
                "आदरणीय महोदय,\n\n" +
                "मी [Applicant Name], आपल्या महाविद्यालयात [Address / Department] या वर्गात शिक्षण घेत असून माझा हजेरी क्रमांक / नोंदणी क्रमांक [Roll Number] असा आहे.\n\n" +
                "मला [Purpose] या कामासाठी महाविद्यालयाचे अधिकृत बोनाफाईड प्रमाणपत्र सादर करणे आवश्यक आहे. तरी मला कृपया बोनाफाईड प्रमाणपत्र देण्यात यावे ही नम्र विनंती.\n\n" +
                "या अर्जासोबत मी माझ्या ओळखपत्राची प्रत व चालू शैक्षणिक वर्षाची फी पावती जोडलेली आहे.\n\n" +
                "कळावे,\n\n" +
                "आपला/आपली नम्र,\n" +
                "[Applicant Name]\n" +
                "हजेरी क्र.: [Roll Number]\n" +
                "मोबाईल: [Contact Number]",
                false, true
        ), "महाविद्यालय ओळखपत्र, चालू वर्षाची फी पावती", "महाविद्यालयाच्या विद्यार्थी विभाग (Student Section) मध्ये सादर करावा.");

        seedTemplate(new ApplicationTemplate(
                "Transfer Certificate (TC) Application",
                "transfer-certificate-application",
                "Application to obtain College Leaving Certificate / Transfer Certificate after course completion.",
                "Request College Transfer Certificate (TC)",
                "en", college,
                "To,\n" +
                "The Principal,\n" +
                "[College Name],\n" +
                "[Address]\n\n" +
                "Date: [Date]\n\n" +
                "Subject: Application for issuance of Transfer Certificate (T.C.)\n\n" +
                "Respected Sir/Madam,\n\n" +
                "I was a student at your college and have successfully completed my studies in [Address / Department] during the academic year with Roll/PRN No: [Roll Number].\n\n" +
                "I am now seeking admission to higher education / relocation, hence I require my Transfer Certificate. I have cleared all pending library and department dues (No Dues certificate attached).\n\n" +
                "Kindly issue my Transfer Certificate at your earliest convenience.\n\n" +
                "Thank you.\n\n" +
                "Yours faithfully,\n" +
                "[Applicant Name]\n" +
                "Contact: [Contact Number]",
                false, true
        ), "No Dues Form, Final Year Marksheet, College ID Card", "Clear library and laboratory dues before submission.");

        seedTemplate(new ApplicationTemplate(
                "महाविद्यालय सोडल्याचा दाखला अर्ज (TC / LC Application)",
                "college-tc-marathi",
                "महाविद्यालय सोडल्याचा दाखला (Transfer Certificate / Leaving Certificate) मागणी अर्ज.",
                "महाविद्यालय सोडल्याचा दाखला मिळण्याबाबत",
                "mr", college,
                "प्रति,\n" +
                "मा. प्राचार्य महोदय,\n" +
                "[College Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: महाविद्यालय सोडल्याचा दाखला (TC / LC) मिळण्याबाबत.\n\n" +
                "महोदय,\n\n" +
                "मी [Applicant Name], आपल्या महाविद्यालयात [Address / Department] या शाखेत विद्यार्थी होतो/होते. माझा नोंदणी क्रमांक [Roll Number] आहे. मी शैक्षणिक वर्षामध्ये अंतिम परीक्षा उत्तीर्ण झालेलो आहे.\n\n" +
                "मला पुढील शिक्षणासाठी [Purpose] येथे प्रवेश घ्यावयाचा असल्याने मला महाविद्यालय सोडल्याचा दाखला (TC) आवश्यक आहे. मी ग्रंथालय व इतर सर्व विभागांचे ना-हरकत प्रमाणपत्र (No Dues) जोडले आहे.\n\n" +
                "कृपया मला दाखला लवकरात लवकर द्यावा ही विनंती.\n\n" +
                "आपला नम्र विद्यार्थी,\n" +
                "[Applicant Name]\n" +
                "मोबाईल: [Contact Number]",
                false, true
        ), "ना-हरकत प्रमाणपत्र (No Dues), अंतिम गुणपत्रिका प्रत, ओळखपत्र", "ग्रंथालय व प्रयोगशाळेची थकबाकी नसल्याची खात्री करा.");

        seedTemplate(new ApplicationTemplate(
                "No Objection Certificate (NOC) Application",
                "noc-certificate-application",
                "Formal application requesting a No Objection Certificate from college for internships or passport.",
                "Request No Objection Certificate (NOC)",
                "en", college,
                "To,\n" +
                "The Head of Department / Principal,\n" +
                "[College Name],\n" +
                "[Address]\n\n" +
                "Date: [Date]\n\n" +
                "Subject: Request for issuance of No Objection Certificate (NOC)\n\n" +
                "Respected Sir/Madam,\n\n" +
                "I am [Applicant Name], currently enrolled in [Address / Department], Roll Number [Roll Number].\n\n" +
                "I have been selected for [Purpose] / require an NOC for official documentation. I assure you that this engagement will not compromise my academic attendance or exams.\n\n" +
                "I request you to kindly issue a No Objection Certificate in my favor.\n\n" +
                "Thanking you.\n\n" +
                "Yours obediently,\n" +
                "[Applicant Name]\n" +
                "Roll No: [Roll Number]\n" +
                "Contact: [Contact Number]",
                false, true
        ), "Offer Letter / Verification letter, ID Card Copy", "Attach relevant internship or scholarship offer letter.");

        seedTemplate(new ApplicationTemplate(
                "Duplicate ID Card Request Application",
                "duplicate-id-card-application",
                "Application requesting the issuance of a duplicate college identity card due to loss.",
                "Request for Duplicate College Identity Card",
                "en", college,
                "To,\n" +
                "The Principal / Registrar,\n" +
                "[College Name],\n" +
                "[Address]\n\n" +
                "Date: [Date]\n\n" +
                "Subject: Application for Duplicate College Identity Card\n\n" +
                "Respected Sir/Madam,\n\n" +
                "I am [Applicant Name], studying in [Address / Department], Roll No: [Roll Number].\n\n" +
                "I regret to inform you that I have unfortunately misplaced / lost my original college identity card on [Date]. Despite diligent efforts, I have been unable to find it. I require an identity card for campus entry, library access, and examinations.\n\n" +
                "I have deposited the requisite duplicate card issuance fee. Kindly issue me a duplicate identity card.\n\n" +
                "Yours faithfully,\n" +
                "[Applicant Name]\n" +
                "Roll No: [Roll Number]\n" +
                "Contact: [Contact Number]",
                false, true
        ), "Challan of duplicate card fee, Passport size photo", "Submit at the College Admin Office.");

        // --- TAHSIL OFFICE ---
        seedTemplate(new ApplicationTemplate(
                "उत्पन्न दाखला अर्ज (Income Certificate Application Format)",
                "income-certificate-tahsil-marathi",
                "तहसीलदार कार्यालयाकडे वार्षिक कौटुंबिक उत्पन्नाचा दाखला मिळण्याकरिता करावयाचा नमुना अर्ज.",
                "तहसीलदार कार्यालयाकडून उत्पन्नाचा दाखला मिळण्याबाबत",
                "mr", tahsil,
                "प्रति,\n" +
                "मा. तहसीलदार साहेब,\n" +
                "तहसील कार्यालय, [Office Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: वार्षिक उत्पन्नाचा दाखला मिळण्याबाबत अर्ज.\n\n" +
                "अर्जदार: [Applicant Name]\n" +
                "पत्ता: [Address]\n\n" +
                "महोदय,\n\n" +
                "मी वरील पत्त्यावर राहणारा/राहणारी असून मला [Purpose] (उदा. शैक्षणिक शिष्यवृत्ती / शासकीय योजना) कामासाठी सन २०२५-२०२६ या वर्षाचा अधिकृत उत्पन्नाचा दाखला आवश्यक आहे.\n\n" +
                "माझ्या कुटुंबाचे सर्व मार्ग मिळून वार्षिक उत्पन्न रुपये [Roll Number] (अक्षरी: [Roll Number]) इतके आहे. या संदर्भातील स्वयंघोषणापत्र, तलाठी अहवाल आणि उत्पन्नाचा पुरावा मी या अर्जासोबत जोडलेला आहे.\n\n" +
                "तरी कृपया मला विहित मुदतीत उत्पन्नाचा दाखला मिळावा ही विनंती.\n\n" +
                "आपला/आपली नम्र,\n" +
                "[Applicant Name]\n" +
                "मोबाईल क्र.: [Contact Number]",
                false, true
        ), "तलाठी अहवाल, रेशन कार्ड प्रत, आधार कार्ड, उत्पन्नाचे स्वयंघोषणापत्र", "नोंदणीकृत महा-ई-सेवा केंद्र किंवा तहसील सेतू केंद्रात सादर करावा.");

        seedTemplate(new ApplicationTemplate(
                "जात प्रमाणपत्र अर्ज नमुना (Caste Certificate Request Format)",
                "caste-certificate-tahsil-marathi",
                "तहसीलदार / उपविभागीय अधिकारी कार्यालयाकडे जात प्रमाणपत्र मागणीसाठी सादर करावयाचा प्रारंभीक नमुना अर्ज.",
                "जात प्रमाणपत्र मागणी अर्ज",
                "mr", tahsil,
                "प्रति,\n" +
                "मा. उपविभागीय अधिकारी / तहसीलदार साहेब,\n" +
                "तहसील कार्यालय, [Office Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: जात प्रमाणपत्र (Caste Certificate) मिळण्याबाबत अर्ज.\n\n" +
                "अर्जदार: [Applicant Name]\n\n" +
                "महोदय,\n\n" +
                "मी [Applicant Name], रा. [Address], विनंतीपूर्वक अर्ज करतो/करते की मी [Roll Number] या प्रवर्गातील [Purpose] जातीचा/जातीची आहे. मला शासकीय शैक्षणिक प्रवेश व नोकरीसाठी जात प्रमाणपत्र आवश्यक आहे.\n\n" +
                "माझे व वडिलांचे प्राथमिक शाळा सोडल्याचा दाखला, १९६७ पूर्वीचा महसुली पुरावा, आणि आधार कार्ड अर्जासोबत जोडले आहे. कृपया मला जात प्रमाणपत्र मंजूर करावे ही नम्र विनंती.\n\n" +
                "आपला नम्र,\n" +
                "[Applicant Name]\n" +
                "मोबाईल: [Contact Number]",
                false, true
        ), "शाळा सोडल्याचा दाखला (LC), वडिलांचा LC, १९६७ पूर्वीचा पुरावा, आधार कार्ड", "तहसील सेतू केंद्र किंवा आपले सरकार पोर्टलवर पडताळणी करावी.");

        seedTemplate(new ApplicationTemplate(
                "रहिवासी दाखला अर्ज (Residence Certificate Application)",
                "residence-certificate-marathi",
                "तहसील कार्यालयाकडून अधिवास व रहिवासी दाखला (Domicile / Residence) मिळण्याबाबत नमुना अर्ज.",
                "रहिवासी / अधिवास प्रमाणपत्र मिळण्याबाबत",
                "mr", tahsil,
                "प्रति,\n" +
                "मा. तहसीलदार साहेब,\n" +
                "तहसील कार्यालय, [Office Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: रहिवासी दाखला (Residence Certificate) मिळण्याबाबत.\n\n" +
                "महोदय,\n\n" +
                "मी [Applicant Name], मागील १५ वर्षांहून अधिक काळापासून [Address] या पत्त्यावर कुटुंबासह वास्तव्यास आहे. मला [Purpose] यासाठी रहिवासी पुरावा सादर करावयाचा आहे.\n\n" +
                "मी वास्तव्याचा पुरावा म्हणून ग्रामपंचायत/मनपा दाखला, लाईट बिल, आणि शिधापत्रिका (Ration Card) जोडले आहे. कृपया मला रहिवासी प्रमाणपत्र द्यावे ही विनंती.\n\n" +
                "आपला नम्र,\n" +
                "[Applicant Name]\n" +
                "मोबाईल: [Contact Number]",
                false, true
        ), "लाईट बिल, रेशन कार्ड, मतदान कार्ड, १० वर्षांचा वास्तव्याचा पुरावा", "तहसीलदार कार्यालयात किंवा अधिकृत सेतू केंद्रात जमा करावा.");

        seedTemplate(new ApplicationTemplate(
                "General Request Letter to Tahsildar (तहसीलदार सामान्य अर्ज)",
                "general-tahsildar-request-marathi",
                "तहसीलदारांकडे महसूल, फेरफार नोंद किंवा कार्यालयीन कामासाठी करावयाचा सर्वसाधारण अर्ज.",
                "तहसीलदारांकडे महसूल किंवा शासकीय सेवेबाबत सर्वसाधारण निवेदन",
                "mr", tahsil,
                "प्रति,\n" +
                "मा. तहसीलदार साहेब,\n" +
                "तहसील कार्यालय, [Office Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: [Purpose] बाबत विनंती अर्ज.\n\n" +
                "अर्जदार: [Applicant Name]\n" +
                "पत्ता: [Address]\n\n" +
                "महोदय,\n\n" +
                "मी [Applicant Name], वरील पत्त्यावर राहत असून मी आपणास सादर करतो की [Purpose]. या संदर्भातील मूळ कागदपत्रे व फेरफार उतारे मी सोबत जोडत आहे.\n\n" +
                "तरी महोदयांनी या प्रकरणात उचित चौकशी करून आवश्यक ती कार्यवाही करावी ही नम्र विनंती.\n\n" +
                "आपला नम्र,\n" +
                "[Applicant Name]\n" +
                "मोबाईल: [Contact Number]",
                false, true
        ), "विषयानुषंगिक ७/१२ उतारा, आधार कार्ड, संबंधित कागदपत्रे", "तहसील आवक-जावक शाखेत पावतीसह जमा करावा.");

        // --- GRAM PANCHAYAT ---
        seedTemplate(new ApplicationTemplate(
                "ग्रामपंचायत रहिवासी दाखला मागणी अर्ज",
                "gram-panchayat-residence-marathi",
                "ग्रामपंचायतीच्या ग्रामसेवकाकडे गावात राहत असल्याचा रहिवासी दाखला मागणी अर्ज.",
                "ग्रामपंचायत हद्दीतील रहिवासी प्रमाणपत्र मागणी",
                "mr", panchayat,
                "प्रति,\n" +
                "मा. ग्रामसेवक / सरपंच साहेब,\n" +
                "ग्रामपंचायत कार्यालय, [Office Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: गावात राहत असल्याबाबत रहिवासी दाखला मिळण्याबाबत.\n\n" +
                "अर्जदार: [Applicant Name], रा. [Address]\n\n" +
                "महोदय,\n\n" +
                "मी [Applicant Name], आपल्या ग्रामपंचायत हद्दीतील घर नं. [Roll Number] येथे मागील अनेक वर्षांपासून कायमस्वरूपी वास्तव्यास आहे. मला [Purpose] या कामासाठी ग्रामपंचायतीचा रहिवासी दाखला आवश्यक आहे.\n\n" +
                "माझ्या नावावर घरपट्टी व पाणीपट्टी पावती भरलेली असून कोणतीही थकबाकी नाही. तरी कृपया मला रहिवासी दाखला द्यावा ही नम्र विनंती.\n\n" +
                "आपला नम्र ग्रामस्थ,\n" +
                "[Applicant Name]\n" +
                "मोबाईल: [Contact Number]",
                false, true
        ), "घरपट्टी पावती, वीज बिल, आधार कार्ड", "ग्रामसेवक यांच्याकडे स्वाक्षरीसाठी द्यावा.");

        seedTemplate(new ApplicationTemplate(
                "जन्म नोंद प्रमाणपत्र मागणी अर्ज (Gram Panchayat Birth Record)",
                "gram-panchayat-birth-record-marathi",
                "ग्रामपंचायत दफ्तरी नोंद असलेल्या जन्म दाखल्याची सत्यप्रत मिळण्याबाबत अर्ज.",
                "ग्रामपंचायत जन्म नोंद दाखला मिळण्याबाबत",
                "mr", panchayat,
                "प्रति,\n" +
                "मा. ग्रामसेवक साहेब,\n" +
                "ग्रामपंचायत कार्यालय, [Office Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: जन्म नोंद दाखल्याची अधिकृत प्रत मिळण्याबाबत.\n\n" +
                "महोदय,\n\n" +
                "मी [Applicant Name], रा. [Address], विनंती करतो की माझ्या पाल्याचा/माझा जन्म आपल्या ग्रामपंचायत हद्दीत दिनांक [Date] रोजी झालेला असून त्याची नोंद ग्रामपंचायत जन्म नोंदवहीत आहे.\n\n" +
                "मला शाळेत दाखल करण्यासाठी जन्म दाखल्याची आवश्यकता आहे. तरी कृपया नमुना ५ मधील अधिकृत जन्म नोंद प्रमाणपत्र मला मिळावे.\n\n" +
                "आपला नम्र,\n" +
                "[Applicant Name]\n" +
                "मोबाईल: [Contact Number]",
                false, true
        ), "दवाखाना डिस्चार्ज कार्ड, माता-पित्याचे आधार कार्ड", "ग्रामपंचायत आरोग्य शाखेत जमा करा.");

        // --- HOSPITAL ---
        seedTemplate(new ApplicationTemplate(
                "Medical Certificate Request Application",
                "medical-certificate-request-en",
                "Formal application addressed to the Medical Superintendent / Doctor requesting a Medical Fitness or Illness Certificate.",
                "Request Medical Certificate / Fitness Certificate",
                "en", hospital,
                "To,\n" +
                "The Medical Superintendent / Doctor,\n" +
                "[Office Name],\n" +
                "[Address]\n\n" +
                "Date: [Date]\n\n" +
                "Subject: Request for issuance of Medical Certificate\n\n" +
                "Respected Doctor,\n\n" +
                "I am [Applicant Name], residing at [Address]. I underwent medical treatment at your hospital under OPD / IPD Registration No: [Roll Number] for the ailment of [Purpose].\n\n" +
                "I need to submit an authentic Medical Certificate to my employer / college to regularize my medical leave from [Date] to [Date].\n\n" +
                "I request you to kindly examine my case history and issue the Medical Certificate.\n\n" +
                "Thanking you.\n\n" +
                "Yours sincerely,\n" +
                "[Applicant Name]\n" +
                "Contact: [Contact Number]",
                false, true
        ), "Hospital OPD/IPD Case Card, Treatment Summary, Government ID", "Present before the treating physician during OPD hours.");

        seedTemplate(new ApplicationTemplate(
                "Medical Records & Discharge Summary Request",
                "medical-records-request-en",
                "Application to the Medical Records Department (MRD) for duplicate case papers and discharge summary.",
                "Request Copy of Hospital Medical Records",
                "en", hospital,
                "To,\n" +
                "The In-Charge, Medical Records Department (MRD),\n" +
                "[Office Name],\n" +
                "[Address]\n\n" +
                "Date: [Date]\n\n" +
                "Subject: Application for Duplicate Discharge Summary and Medical Records\n\n" +
                "Respected Sir/Madam,\n\n" +
                "Patient Name: [Applicant Name]\n" +
                "Admission No / IPD: [Roll Number]\n" +
                "Date of Admission/Discharge: [Date]\n\n" +
                "I am the patient / authorized relative of the patient. I require certified duplicate copies of the Discharge Summary, Lab Reports, and Doctor Notes for the purpose of [Purpose] (insurance reimbursement / second medical opinion).\n\n" +
                "I have attached the patient ID proof and relation proof. Please issue the certified records at the earliest.\n\n" +
                "Yours faithfully,\n" +
                "[Applicant Name]\n" +
                "Contact: [Contact Number]",
                false, true
        ), "Original Discharge Card Copy, Patient Aadhar Card, Authorization letter", "Submit at the MRD counter with applicable photocopying fees.");

        // --- BANK ---
        seedTemplate(new ApplicationTemplate(
                "Bank Statement Request Application",
                "bank-statement-request-en",
                "Formal customer letter to the Bank Branch Manager requesting an authorized account statement.",
                "Request Bank Account Statement for Visa / Loan / Audit",
                "en", bank,
                "To,\n" +
                "The Branch Manager,\n" +
                "[Office Name],\n" +
                "[Address]\n\n" +
                "Date: [Date]\n\n" +
                "Subject: Request for Bank Account Statement\n\n" +
                "Respected Sir/Madam,\n\n" +
                "Account Holder Name: [Applicant Name]\n" +
                "Account Number: [Roll Number]\n" +
                "Branch: [Address]\n\n" +
                "I hold a Savings / Current Account with your branch. I require a certified bank account statement with branch seal and signature for the period [Date] to [Date] for the purpose of [Purpose].\n\n" +
                "Kindly debit the applicable statement charges, if any, from my account and provide the sealed statement.\n\n" +
                "Thanking you.\n\n" +
                "Yours faithfully,\n" +
                "[Applicant Name]\n" +
                "Contact: [Contact Number]",
                false, true
        ), "Passbook Copy, Aadhar Card / PAN Card", "Submit at the Customer Helpdesk of your home branch.");

        seedTemplate(new ApplicationTemplate(
                "बँकेत पत्ता बदल अर्ज (Bank Address Change Request)",
                "bank-address-change-marathi",
                "बँक खात्यामध्ये नवीन पत्रव्यवहाराचा पत्ता नोंदवण्यासाठी शाखा व्यवस्थापकांकडे करावयाचा अर्ज.",
                "बँक खात्यात पत्ता बदल करण्याबाबत",
                "mr", bank,
                "प्रति,\n" +
                "मा. शाखा व्यवस्थापक साहेब,\n" +
                "[Office Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: बँक खात्यात नवीन पत्ता नोंदवण्याबाबत अर्ज.\n\n" +
                "खातेदाराचे नाव: [Applicant Name]\n" +
                "खाते क्रमांक: [Roll Number]\n\n" +
                "महोदय,\n\n" +
                "माझे आपल्या शाखेत वरील क्रमांकाचे बचत खाते आहे. मी नुकतेच स्थलांतर केले असून माझा नवीन पत्ता पुढीलप्रमाणे आहे:\n" +
                "नवीन पत्ता: [Address]\n\n" +
                "माझ्या बँक खात्यावर नवीन पत्ता अद्ययावत (Update) करावा ही विनंती. पुराव्यासाठी मी नवीन आधार कार्ड व वीज बिलाची प्रत जोडली आहे.\n\n" +
                "आपला नम्र ग्राहक,\n" +
                "[Applicant Name]\n" +
                "मोबाईल: [Contact Number]",
                false, true
        ), "आधार कार्ड, नवीन पत्त्याचे वीज बिल, पासबुक प्रत", "शाखेत मूळ कागदपत्रांसह प्रत्यक्ष सादर करावा.");

        // --- MUNICIPAL CORPORATION ---
        seedTemplate(new ApplicationTemplate(
                "महानगरपालिका नागरी तक्रार अर्ज (Civic Grievance Application)",
                "municipal-civic-grievance-marathi",
                "रस्ते, कचरा, पाणीपुरवठा किंवा पथदिवे संदर्भात मनपा प्रभाग कार्यालयाकडे करावयाची तक्रार.",
                "महानगरपालिकेकडे नागरी समस्येबाबत तक्रार निवेदन",
                "mr", municipal,
                "प्रति,\n" +
                "मा. सहाय्यक आयुक्त / प्रभाग अधिकारी,\n" +
                "महानगरपालिका प्रभाग कार्यालय, [Office Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: प्रभागातील [Purpose] समस्येबाबत तातडीने उपाययोजना करणेबाबत.\n\n" +
                "अर्जदार: [Applicant Name], पत्ता: [Address]\n\n" +
                "महोदय,\n\n" +
                "मी आपल्या प्रभाग परिसरातील नागरिक असून आमच्या भागात [Purpose] ची गंभीर समस्या निर्माण झाली आहे. यामुळे स्थानिक ज्येष्ठ नागरिक व विद्यार्थ्यांना मोठा त्रास सहन करावा लागत आहे.\n\n" +
                "या संदर्भात महापालिकेने संबंधित अभियंत्यांना पाठवून प्रत्यक्ष पाहणी करावी व समस्येचे तातडीने निवारण करावे ही नम्र विनंती.\n\n" +
                "आपला विश्वासू,\n" +
                "[Applicant Name]\n" +
                "मोबाईल: [Contact Number]",
                false, true
        ), "समस्येचे छायाचित्र (असल्यास), परिसरातील नागरिकांची स्वाक्षरी", "मनपा नागरिक सुविधा केंद्रात सादर करा.");

        // --- POLICE STATION ---
        seedTemplate(new ApplicationTemplate(
                "कागदपत्र हरवल्याची तक्रार / नोंद (Lost Document Intimation)",
                "police-lost-document-marathi",
                "शाळा सोडल्याचा दाखला, मार्कशीट किंवा पाकीट हरवल्यास पोलीस ठाण्यात करावयाची अधिकृत नोंद.",
                "महत्त्वाची कागदपत्रे हरवल्याबाबत पोलीस ठाण्यात नोंद अर्ज",
                "mr", police,
                "प्रति,\n" +
                "मा. वरिष्ठ पोलीस निरीक्षक साहेब,\n" +
                "पोलीस स्टेशन, [Office Name],\n" +
                "[Address]\n\n" +
                "दिनांक: [Date]\n\n" +
                "विषय: माझे [Purpose] हरवल्याबाबत रीतसर नोंद होण्याबाबत.\n\n" +
                "अर्जदार: [Applicant Name], पत्ता: [Address]\n\n" +
                "महोदय,\n\n" +
                "मी [Applicant Name], विनंतीपूर्वक नमूद करतो/करते की दिनांक [Date] रोजी [Address] या परिसरात जात असताना माझे [Purpose] (क्रमांक: [Roll Number]) अज्ञात कारणाने हरवले आहे. मी सर्व ठिकाणी शोध घेतला परंतु ते सापडले नाही.\n\n" +
                "सदर हरवलेल्या कागदपत्रांचा कोणताही गैरवापर होऊ नये व मला डुप्लिकेट प्रत मिळण्यासाठी ही तक्रार दाखल करत आहे. मला या अर्जाची पोहोच पावती मिळावी ही नम्र विनंती.\n\n" +
                "आपला नम्र,\n" +
                "[Applicant Name]\n" +
                "मोबाईल: [Contact Number]",
                false, true
        ), "ओळखपत्र प्रत, हरवलेल्या कागदपत्राची झेरॉक्स प्रत (असल्यास)", "पोलीस ठाण्याच्या ठाणे अंमलदार (Duty Officer) कडे सादर करावा.");

        seedTemplate(new ApplicationTemplate(
                "General Complaint Letter (Police Intimation)",
                "police-general-complaint-en",
                "Formal written application to the Police Inspector regarding civic nuisance or disturbance.",
                "Written Intimation / Complaint to Local Police Station",
                "en", police,
                "To,\n" +
                "The Senior Police Inspector,\n" +
                "[Office Name],\n" +
                "[Address]\n\n" +
                "Date: [Date]\n\n" +
                "Subject: Formal complaint regarding [Purpose]\n\n" +
                "Applicant: [Applicant Name], Address: [Address]\n\n" +
                "Respected Sir,\n\n" +
                "I am a resident of the above mentioned address. I would like to bring to your urgent attention the matter of [Purpose] occurring in our locality.\n\n" +
                "This situation is causing severe inconvenience, safety hazards, and distress to law-abiding residents. I request you to take necessary preventive and patrolling measures to ensure peace and security in our neighborhood.\n\n" +
                "Thanking you.\n\n" +
                "Yours faithfully,\n" +
                "[Applicant Name]\n" +
                "Contact: [Contact Number]",
                false, true
        ), "Government ID Proof, Proof of Address", "Submit at the local police station and obtain a stamped receiving acknowledgment.");

        // --- COURT ---
        seedTemplate(new ApplicationTemplate(
                "Certified Copy Request Application (न्यायालय प्रमाणित प्रत मागणी)",
                "court-certified-copy-en",
                "Application to the Court Superintendent / Registrar requesting certified copies of an order or judgment.",
                "Request Certified Copy of Court Order / Decree",
                "en", court,
                "To,\n" +
                "The Hon'ble Superintendent / Registrar of Copying Section,\n" +
                "District & Sessions Court, [Office Name],\n" +
                "[Address]\n\n" +
                "Date: [Date]\n\n" +
                "Subject: Application for issuance of Certified Copy of Order / Judgment\n\n" +
                "Case Title: [Applicant Name] Versus [Roll Number]\n" +
                "Case / Special Civil Suit No: [Roll Number]\n\n" +
                "Respected Sir/Madam,\n\n" +
                "I am [Applicant Name], the Plaintiff / Defendant / Authorized Person in the aforementioned matter.\n\n" +
                "I humbly pray for the issuance of an urgent Certified True Copy of the Order / Judgment dated [Date] passed by the Hon'ble Court for the purpose of [Purpose].\n\n" +
                "I undertake to pay the requisite court copying fees and court fee stamps as per rules.\n\n" +
                "Yours obediently,\n" +
                "[Applicant Name]\n" +
                "Contact: [Contact Number]",
                false, true
        ), "Court Fee Stamps, Case Number details, Vakalatnama / ID proof", "Submit at the Copying Department of the concerned Court.");

        // --- OTHER INSTITUTIONS ---
        seedTemplate(new ApplicationTemplate(
                "Hostel Admission Application (वसतिगृह प्रवेश अर्ज)",
                "hostel-admission-request-en",
                "Standard application to the Hostel Rector / Warden requesting student accommodation.",
                "Request Student Hostel Accommodation",
                "en", other,
                "To,\n" +
                "The Rector / Chief Warden,\n" +
                "[Office Name],\n" +
                "[Address]\n\n" +
                "Date: [Date]\n\n" +
                "Subject: Application for Student Hostel Room Allotment\n\n" +
                "Respected Sir/Madam,\n\n" +
                "I am [Applicant Name], a newly admitted student in [Address / Department] with Merit / Roll No: [Roll Number]. My permanent hometown is [Address], which is located far from the institute campus.\n\n" +
                "I request you to kindly grant me accommodation in the college/institutional hostel for the upcoming academic year. I solemnly promise to abide by all hostel rules and discipline regulations.\n\n" +
                "Enclosed are my admission receipt and permanent address proof.\n\n" +
                "Thanking you.\n\n" +
                "Yours obediently,\n" +
                "[Applicant Name]\n" +
                "Contact: [Contact Number]",
                false, true
        ), "College Admission Receipt, Hometown Address Proof, Medical Fitness Certificate", "Submit to the Hostel Warden Office.");

        log.info("Successfully seeded 10 Office Categories and comprehensive initial templates!");
    }

    private void seedTemplate(ApplicationTemplate template, String requiredDocs, String instructions) {
        if (!templateRepository.existsBySlug(template.getSlug())) {
            template.setRequiredDocuments(requiredDocs);
            template.setInstructions(instructions);
            templateRepository.save(template);
        }
    }
}
