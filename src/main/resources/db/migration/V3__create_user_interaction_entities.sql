-- V3__create_user_interaction_entities.sql
-- User bookmarks, follows, notifications, reports, and API sources

-- Bookmark table
CREATE TABLE IF NOT EXISTS bookmarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    paper_id BIGINT,
    keyword_id BIGINT,
    bookmark_type ENUM('PAPER', 'KEYWORD') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (paper_id) REFERENCES research_papers(id) ON DELETE CASCADE,
    FOREIGN KEY (keyword_id) REFERENCES keywords(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_paper (user_id, paper_id),
    UNIQUE KEY uk_user_keyword (user_id, keyword_id),
    INDEX idx_bookmarks_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- UserFollow table (follow journals or topics)
CREATE TABLE IF NOT EXISTS user_follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    journal_id BIGINT,
    topic_id BIGINT,
    follow_type ENUM('JOURNAL', 'TOPIC') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (journal_id) REFERENCES journals(id) ON DELETE CASCADE,
    FOREIGN KEY (topic_id) REFERENCES research_topics(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_journal (user_id, journal_id),
    UNIQUE KEY uk_user_topic (user_id, topic_id),
    INDEX idx_follows_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notification table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type ENUM('NEW_PAPER', 'TRENDING', 'JOURNAL_UPDATE', 'SYSTEM') NOT NULL,
    title VARCHAR(500) NOT NULL,
    message TEXT,
    reference_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_user (user_id),
    INDEX idx_notifications_read (is_read),
    INDEX idx_notifications_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- DashboardReport table
CREATE TABLE IF NOT EXISTS dashboard_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    report_type ENUM('TREND_SUMMARY', 'TOPIC_ANALYSIS', 'JOURNAL_SUMMARY', 'AUTHOR_ANALYSIS') NOT NULL,
    title VARCHAR(500) NOT NULL,
    content JSON,
    parameters JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_reports_user (user_id),
    INDEX idx_reports_type (report_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ApiDataSource table
CREATE TABLE IF NOT EXISTS api_data_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_name VARCHAR(100) NOT NULL UNIQUE,
    base_url VARCHAR(500) NOT NULL,
    api_key VARCHAR(500),
    rate_limit_per_day INT DEFAULT 1000,
    is_active BOOLEAN DEFAULT TRUE,
    last_sync_at DATETIME,
    last_sync_status ENUM('SUCCESS', 'FAILED', 'PARTIAL') DEFAULT NULL,
    records_synced INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sources_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User recent searches (for tracking and suggestions)
CREATE TABLE IF NOT EXISTS recent_searches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    search_query VARCHAR(500) NOT NULL,
    search_type ENUM('PAPER', 'AUTHOR', 'JOURNAL', 'KEYWORD') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_searches_user (user_id),
    INDEX idx_searches_query (search_query)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
