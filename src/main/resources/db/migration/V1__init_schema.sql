-- V1__init_schema.sql
-- Baseline schema for a fresh TrendSearchor database.

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    dob DATE,
    mail VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    gender VARCHAR(255),
    workplace VARCHAR(200),
    role VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME,
    updated_at DATETIME,
    last_login DATETIME,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_mail UNIQUE (mail)
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_mail ON users (mail);
CREATE INDEX idx_users_role_status ON users (role, status);

CREATE TABLE research_papers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(255),
    title VARCHAR(1000) NOT NULL,
    abstract_text TEXT,
    `year` INT,
    citation_count INT DEFAULT 0,
    open_access BOOLEAN DEFAULT FALSE,
    paper_uri VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT uk_research_papers_external_id UNIQUE (external_id)
);

CREATE INDEX idx_papers_year ON research_papers (`year`);
CREATE INDEX idx_papers_external_id ON research_papers (external_id);

CREATE TABLE journals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(255),
    name VARCHAR(500) NOT NULL,
    publisher VARCHAR(500),
    issn VARCHAR(20),
    country VARCHAR(100),
    homepage_url VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT uk_journals_external_id UNIQUE (external_id)
);

CREATE INDEX idx_journals_external_id ON journals (external_id);
CREATE INDEX idx_journals_name ON journals (name);

CREATE TABLE authors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(255),
    name VARCHAR(500) NOT NULL,
    orcid VARCHAR(50),
    h_index INT DEFAULT 0,
    paper_count INT DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT uk_authors_external_id UNIQUE (external_id)
);

CREATE INDEX idx_authors_external_id ON authors (external_id);
CREATE INDEX idx_authors_name ON authors (name);

CREATE TABLE keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    created_at DATETIME,
    CONSTRAINT uk_keywords_name UNIQUE (name)
);

CREATE INDEX idx_keywords_name ON keywords (name);

CREATE TABLE research_topics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255),
    popularity_score DOUBLE DEFAULT 0.0,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT uk_research_topics_name UNIQUE (name)
);

CREATE INDEX idx_topics_name ON research_topics (name);
CREATE INDEX idx_topics_category ON research_topics (category);

CREATE TABLE publication_trends (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword_name VARCHAR(255) NOT NULL,
    `year` INT NOT NULL,
    `month` INT,
    paper_count INT DEFAULT 0,
    citation_count INT DEFAULT 0,
    avg_citations DOUBLE DEFAULT 0.0,
    created_at DATETIME,
    CONSTRAINT uk_trend_keyword_year_month UNIQUE (keyword_name, `year`, `month`)
);

CREATE INDEX idx_trends_keyword ON publication_trends (keyword_name);
CREATE INDEX idx_trends_year ON publication_trends (`year`);

CREATE TABLE paper_journals (
    paper_id BIGINT NOT NULL,
    journal_id BIGINT NOT NULL,
    PRIMARY KEY (paper_id, journal_id),
    CONSTRAINT fk_paper_journals_paper FOREIGN KEY (paper_id) REFERENCES research_papers (id) ON DELETE CASCADE,
    CONSTRAINT fk_paper_journals_journal FOREIGN KEY (journal_id) REFERENCES journals (id) ON DELETE CASCADE
);

CREATE TABLE paper_authors (
    paper_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (paper_id, author_id),
    CONSTRAINT fk_paper_authors_paper FOREIGN KEY (paper_id) REFERENCES research_papers (id) ON DELETE CASCADE,
    CONSTRAINT fk_paper_authors_author FOREIGN KEY (author_id) REFERENCES authors (id) ON DELETE CASCADE
);

CREATE TABLE paper_keywords (
    paper_id BIGINT NOT NULL,
    keyword_id BIGINT NOT NULL,
    PRIMARY KEY (paper_id, keyword_id),
    CONSTRAINT fk_paper_keywords_paper FOREIGN KEY (paper_id) REFERENCES research_papers (id) ON DELETE CASCADE,
    CONSTRAINT fk_paper_keywords_keyword FOREIGN KEY (keyword_id) REFERENCES keywords (id) ON DELETE CASCADE
);

CREATE TABLE paper_topics (
    paper_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    PRIMARY KEY (paper_id, topic_id),
    CONSTRAINT fk_paper_topics_paper FOREIGN KEY (paper_id) REFERENCES research_papers (id) ON DELETE CASCADE,
    CONSTRAINT fk_paper_topics_topic FOREIGN KEY (topic_id) REFERENCES research_topics (id) ON DELETE CASCADE
);

CREATE TABLE bookmarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    paper_id BIGINT,
    keyword_id BIGINT,
    bookmark_type VARCHAR(255) NOT NULL,
    created_at DATETIME,
    CONSTRAINT uk_user_paper UNIQUE (user_id, paper_id),
    CONSTRAINT uk_user_keyword UNIQUE (user_id, keyword_id),
    CONSTRAINT fk_bookmarks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmarks_paper FOREIGN KEY (paper_id) REFERENCES research_papers (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmarks_keyword FOREIGN KEY (keyword_id) REFERENCES keywords (id) ON DELETE CASCADE
);

CREATE INDEX idx_bookmarks_user ON bookmarks (user_id);

CREATE TABLE user_follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    journal_id BIGINT,
    topic_id BIGINT,
    follow_type VARCHAR(255) NOT NULL,
    created_at DATETIME,
    CONSTRAINT uk_user_journal UNIQUE (user_id, journal_id),
    CONSTRAINT uk_user_topic UNIQUE (user_id, topic_id),
    CONSTRAINT fk_user_follows_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_follows_journal FOREIGN KEY (journal_id) REFERENCES journals (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_follows_topic FOREIGN KEY (topic_id) REFERENCES research_topics (id) ON DELETE CASCADE
);

CREATE INDEX idx_follows_user ON user_follows (user_id);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(255) NOT NULL,
    title VARCHAR(500) NOT NULL,
    message TEXT,
    reference_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user ON notifications (user_id);
CREATE INDEX idx_notifications_read ON notifications (is_read);
CREATE INDEX idx_notifications_created ON notifications (created_at);

CREATE TABLE dashboard_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    report_type VARCHAR(255) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content JSON,
    parameters JSON,
    created_at DATETIME,
    CONSTRAINT fk_dashboard_reports_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_reports_user ON dashboard_reports (user_id);
CREATE INDEX idx_reports_type ON dashboard_reports (report_type);

CREATE TABLE api_data_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_name VARCHAR(100) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    api_key VARCHAR(500),
    rate_limit_per_day INT DEFAULT 1000,
    is_active BOOLEAN DEFAULT TRUE,
    last_sync_at DATETIME,
    last_sync_status VARCHAR(255),
    records_synced INT DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT uk_api_data_sources_source_name UNIQUE (source_name)
);

CREATE INDEX idx_sources_active ON api_data_sources (is_active);

CREATE TABLE recent_searches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    search_query VARCHAR(500) NOT NULL,
    search_type VARCHAR(255) NOT NULL,
    created_at DATETIME,
    CONSTRAINT fk_recent_searches_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_searches_user ON recent_searches (user_id);
CREATE INDEX idx_searches_query ON recent_searches (search_query);

CREATE TABLE invalidated_tokens (
    jti VARCHAR(36) NOT NULL PRIMARY KEY,
    expires_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_invalidated_tokens_jti UNIQUE (jti)
);
