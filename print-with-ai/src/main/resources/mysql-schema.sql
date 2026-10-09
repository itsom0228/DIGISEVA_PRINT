-- ========================================================
-- DIGISEVA PRINT - Full MySQL 8 Database Script
-- Run this in MySQL Workbench, phpMyAdmin, or mysql CLI
-- ========================================================

CREATE DATABASE IF NOT EXISTS printwithai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE printwithai_db;

-- 1. Kiosks Table
CREATE TABLE IF NOT EXISTS kiosks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kiosk_identifier VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    state VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kiosk_identifier (kiosk_identifier)
) ENGINE=InnoDB;

-- 2. Administrator Users Table (Password is BCrypt hash of 'admin123')
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_ADMIN',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. Print Jobs Table
CREATE TABLE IF NOT EXISTS print_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_reference VARCHAR(32) NOT NULL UNIQUE,
    tracking_token VARCHAR(64) NOT NULL UNIQUE,
    kiosk_id VARCHAR(50) NOT NULL,
    kiosk_name VARCHAR(150),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    total_pages INT NOT NULL DEFAULT 1,
    selected_pages VARCHAR(200) DEFAULT 'ALL',
    selected_page_count INT NOT NULL DEFAULT 1,
    print_type VARCHAR(30) NOT NULL,
    copies INT NOT NULL DEFAULT 1,
    duplex BOOLEAN NOT NULL DEFAULT FALSE,
    unit_price DECIMAL(10, 2) NOT NULL DEFAULT 2.00,
    duplex_discount DECIMAL(10, 2) DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 2.00,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'DEMO_PENDING',
    job_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    ai_summary TEXT,
    ai_blank_pages_note VARCHAR(500),
    failure_reason VARCHAR(500),
    file_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    file_deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    INDEX idx_public_reference (public_reference),
    INDEX idx_tracking_token (tracking_token),
    INDEX idx_job_status (job_status),
    INDEX idx_kiosk_id (kiosk_id)
) ENGINE=InnoDB;

-- 4. Payment Transactions Table
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    print_job_id BIGINT NOT NULL,
    transaction_reference VARCHAR(64) NOT NULL UNIQUE,
    payment_mode VARCHAR(30) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_job FOREIGN KEY (print_job_id) REFERENCES print_jobs(id) ON DELETE CASCADE,
    INDEX idx_txn_reference (transaction_reference)
) ENGINE=InnoDB;

-- 5. Kiosk Audit and Hardware Logs Table
CREATE TABLE IF NOT EXISTS kiosk_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kiosk_id VARCHAR(50) NOT NULL,
    order_reference VARCHAR(32),
    event_type VARCHAR(50) NOT NULL,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_kiosk (kiosk_id),
    INDEX idx_log_created (created_at)
) ENGINE=InnoDB;

-- Default Demo Data
INSERT INTO kiosks (kiosk_identifier, name, location, city, state, status, created_at)
VALUES 
('TAHSIL001', 'Tahsil Office Demonstration Kiosk', 'Tahsil Office, Haveli Sub-Division, Pune', 'Pune', 'Maharashtra', 'ACTIVE', NOW()),
('COLLEGE001', 'Government Engineering College Kiosk', 'Shivajinagar Campus, Pune', 'Pune', 'Maharashtra', 'ACTIVE', NOW()),
('HOSPITAL001', 'District Civil Hospital Kiosk', 'Civil Hospital Complex, Satara', 'Satara', 'Maharashtra', 'ACTIVE', NOW()),
('LIBRARY001', 'Central Public Library Kiosk', 'Budhwar Peth, Pune', 'Pune', 'Maharashtra', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Default Administrator User (BCrypt password for 'admin123': $2a$10$wT8Kz1xVwXgHk5r6lM1qReEaN9lZ.w57q0sJ5w3jO2iA.h7p6M2Ce)
INSERT INTO admin_users (username, password_hash, full_name, role, enabled, created_at)
VALUES 
('admin', '$2a$10$7Q77T.5p0oK5jVw7z9K6teE9aN0lZ.w57q0sJ5w3jO2iA.h7p6M2C', 'System Administrator', 'ROLE_ADMIN', TRUE, NOW())
ON DUPLICATE KEY UPDATE username=VALUES(username);

CREATE TABLE IF NOT EXISTS office_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    icon VARCHAR(80),
    display_order INT DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cat_slug (slug),
    INDEX idx_cat_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS application_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL UNIQUE,
    description VARCHAR(1000),
    purpose VARCHAR(500),
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    category_id BIGINT NOT NULL,
    template_content TEXT NOT NULL,
    file_path VARCHAR(300),
    file_type VARCHAR(50) DEFAULT 'TEXT_TEMPLATE',
    official_template BOOLEAN NOT NULL DEFAULT FALSE,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    source_name VARCHAR(200),
    source_url VARCHAR(500),
    instructions TEXT,
    required_documents TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tmpl_slug (slug),
    INDEX idx_tmpl_lang (language),
    INDEX idx_tmpl_active (active),
    INDEX idx_tmpl_cat (category_id),
    CONSTRAINT fk_template_category FOREIGN KEY (category_id) REFERENCES office_categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS template_downloads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    downloaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    action_type VARCHAR(40) NOT NULL DEFAULT 'DOWNLOAD',
    kiosk_id VARCHAR(50),
    INDEX idx_dl_tmpl (template_id),
    INDEX idx_dl_time (downloaded_at),
    CONSTRAINT fk_download_template FOREIGN KEY (template_id) REFERENCES application_templates(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

