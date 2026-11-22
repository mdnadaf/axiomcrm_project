-- AcxiomCRM Database Schema Definition
-- Target: MySQL 8.0+

CREATE DATABASE IF NOT EXISTS acxiomcrm_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE acxiomcrm_db;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_count INT NOT NULL DEFAULT 0,
    lockout_end DATETIME NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Customers Table
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    company VARCHAR(150) NULL,
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_to_id BIGINT NULL,
    created_by_id BIGINT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_customers_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_customers_created_by FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_customers_code (customer_code),
    INDEX idx_customers_email (email),
    INDEX idx_customers_phone (phone),
    INDEX idx_customers_assigned (assigned_to_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Leads Table
CREATE TABLE IF NOT EXISTS leads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lead_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NULL,
    company VARCHAR(150) NULL,
    status VARCHAR(50) NOT NULL,
    source VARCHAR(100) NULL,
    notes TEXT NULL,
    expected_value DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    expected_close_date DATE NULL,
    customer_id BIGINT NULL,
    assigned_to_id BIGINT NULL,
    created_by_id BIGINT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_leads_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE SET NULL,
    CONSTRAINT fk_leads_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_leads_created_by FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_leads_code (lead_code),
    INDEX idx_leads_status (status),
    INDEX idx_leads_assigned (assigned_to_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Opportunities Table
CREATE TABLE IF NOT EXISTS opportunities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    customer_id BIGINT NOT NULL,
    lead_id BIGINT NULL,
    stage VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    probability INT NOT NULL,
    weighted_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    expected_close_date DATE NOT NULL,
    description TEXT NULL,
    assigned_to_id BIGINT NULL,
    created_by_id BIGINT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_opps_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_opps_lead FOREIGN KEY (lead_id) REFERENCES leads (id) ON DELETE SET NULL,
    CONSTRAINT fk_opps_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_opps_created_by FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_opp_amount CHECK (amount > 0),
    CONSTRAINT chk_opp_probability CHECK (probability >= 0 AND probability <= 100),
    INDEX idx_opps_stage (stage),
    INDEX idx_opps_status (status),
    INDEX idx_opps_customer (customer_id),
    INDEX idx_opps_assigned (assigned_to_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Follow-ups Table
CREATE TABLE IF NOT EXISTS follow_ups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follow_up_date DATE NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    remarks TEXT NULL,
    completion_notes TEXT NULL,
    rescheduled_date DATE NULL,
    customer_id BIGINT NULL,
    lead_id BIGINT NULL,
    assigned_to_id BIGINT NULL,
    created_by_id BIGINT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_followup_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_followup_lead FOREIGN KEY (lead_id) REFERENCES leads (id) ON DELETE CASCADE,
    CONSTRAINT fk_followup_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_followup_created_by FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_followup_date (follow_up_date),
    INDEX idx_followup_status (status),
    INDEX idx_followup_assigned (assigned_to_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Activities Table
CREATE TABLE IF NOT EXISTS activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NULL,
    activity_date DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL,
    customer_id BIGINT NULL,
    lead_id BIGINT NULL,
    assigned_to_id BIGINT NULL,
    created_by_id BIGINT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_lead FOREIGN KEY (lead_id) REFERENCES leads (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_created_by FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_activity_date (activity_date),
    INDEX idx_activity_status (status),
    INDEX idx_activity_assigned (assigned_to_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Audit Logs Table (Append-only)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    username VARCHAR(150) NULL,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    record_id BIGINT NULL,
    old_value LONGTEXT NULL,
    new_value LONGTEXT NULL,
    ip_address VARCHAR(50) NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_entity (entity_name, record_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
