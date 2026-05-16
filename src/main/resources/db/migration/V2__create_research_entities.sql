-- V2__create_research_entities.sql
-- Core research data models

-- ResearchPaper table
CREATE TABLE IF NOT EXISTS research_papers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(255) UNIQUE,
    title VARCHAR(1000) NOT NULL,
    abstract_text TEXT,
    year INT,
    citation_count INT DEFAULT 0,
    open_access BOOLEAN DEFAULT FALSE,
    paper_uri VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_papers_year (year),
    INDEX idx_papers_external_id (external_id),
    FULLTEXT INDEX idx_papers_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Journal table
CREATE TABLE IF NOT EXISTS journals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(255) UNIQUE,
    name VARCHAR(500) NOT NULL,
    publisher VARCHAR(500),
    issn VARCHAR(20),
    country VARCHAR(100),
    homepage_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_journals_external_id (external_id),
    INDEX idx_journals_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Author table
CREATE TABLE IF NOT EXISTS authors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(255) UNIQUE,
    name VARCHAR(500) NOT NULL,
    orcid VARCHAR(50),
    h_index INT DEFAULT 0,
    paper_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_authors_external_id (external_id),
    INDEX idx_authors_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Keyword table
CREATE TABLE IF NOT EXISTS keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_keywords_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ResearchTopic table
CREATE TABLE IF NOT EXISTS research_topics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(255),
    popularity_score DOUBLE DEFAULT 0.0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_topics_name (name),
    INDEX idx_topics_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Paper-Journal relationship (many-to-many)
CREATE TABLE IF NOT EXISTS paper_journals (
    paper_id BIGINT NOT NULL,
    journal_id BIGINT NOT NULL,
    PRIMARY KEY (paper_id, journal_id),
    FOREIGN KEY (paper_id) REFERENCES research_papers(id) ON DELETE CASCADE,
    FOREIGN KEY (journal_id) REFERENCES journals(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Paper-Author relationship (many-to-many)
CREATE TABLE IF NOT EXISTS paper_authors (
    paper_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    author_position INT DEFAULT 0,
    PRIMARY KEY (paper_id, author_id),
    FOREIGN KEY (paper_id) REFERENCES research_papers(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Paper-Keyword relationship (many-to-many)
CREATE TABLE IF NOT EXISTS paper_keywords (
    paper_id BIGINT NOT NULL,
    keyword_id BIGINT NOT NULL,
    relevance_score DOUBLE DEFAULT 0.0,
    PRIMARY KEY (paper_id, keyword_id),
    FOREIGN KEY (paper_id) REFERENCES research_papers(id) ON DELETE CASCADE,
    FOREIGN KEY (keyword_id) REFERENCES keywords(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Paper-Topic relationship (many-to-many)
CREATE TABLE IF NOT EXISTS paper_topics (
    paper_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    PRIMARY KEY (paper_id, topic_id),
    FOREIGN KEY (paper_id) REFERENCES research_papers(id) ON DELETE CASCADE,
    FOREIGN KEY (topic_id) REFERENCES research_topics(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PublicationTrend table (stores aggregated trend data over time)
CREATE TABLE IF NOT EXISTS publication_trends (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword_name VARCHAR(255) NOT NULL,
    year INT NOT NULL,
    month INT,
    paper_count INT DEFAULT 0,
    citation_count INT DEFAULT 0,
    avg_citations DOUBLE DEFAULT 0.0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trend_keyword_year_month (keyword_name, year, month),
    INDEX idx_trends_keyword (keyword_name),
    INDEX idx_trends_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
