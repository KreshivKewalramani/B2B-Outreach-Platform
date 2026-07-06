-- Reference SQL Schema for MySQL
-- Database Name: cold_email_automation
-- (Note: Spring Boot JPA generates these tables automatically via spring.jpa.hibernate.ddl-auto=update)

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    reset_token VARCHAR(255) NULL,
    reset_token_expiry DATETIME NULL
);

CREATE TABLE IF NOT EXISTS companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255) NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(100) NULL,
    designation VARCHAR(255) NULL,
    industry VARCHAR(255) NULL,
    notes TEXT NULL,
    date_added DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS email_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS smtp_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    use_tls TINYINT(1) DEFAULT 1 NOT NULL,
    use_ssl TINYINT(1) DEFAULT 0 NOT NULL,
    active TINYINT(1) DEFAULT 0 NOT NULL
);

CREATE TABLE IF NOT EXISTS campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_name VARCHAR(255) NOT NULL,
    template_id BIGINT NOT NULL,
    smtp_account_id BIGINT NOT NULL,
    schedule_time DATETIME NULL,
    status VARCHAR(50) NOT NULL,
    min_delay_seconds INT DEFAULT 15 NOT NULL,
    max_delay_seconds INT DEFAULT 45 NOT NULL,
    target_type VARCHAR(50) DEFAULT 'ALL' NOT NULL,
    target_value VARCHAR(255) NULL,
    created_date DATETIME NOT NULL,
    CONSTRAINT fk_campaign_template FOREIGN KEY (template_id) REFERENCES email_templates(id),
    CONSTRAINT fk_campaign_smtp FOREIGN KEY (smtp_account_id) REFERENCES smtp_accounts(id)
);

CREATE TABLE IF NOT EXISTS campaign_recipients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT NULL,
    sent_timestamp DATETIME NULL,
    retry_count INT DEFAULT 0 NOT NULL,
    CONSTRAINT fk_recipient_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_recipient_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE TABLE IF NOT EXISTS email_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    timestamp DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT NULL,
    campaign_id BIGINT NULL,
    smtp_account_id BIGINT NULL,
    CONSTRAINT fk_log_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_log_smtp FOREIGN KEY (smtp_account_id) REFERENCES smtp_accounts(id)
);
