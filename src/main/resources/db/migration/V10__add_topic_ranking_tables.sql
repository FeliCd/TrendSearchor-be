-- V10: Add topic ranking tables for periodic (6-month) ranking snapshots
-- Feature: F1 - Topic Ranking

CREATE TABLE topic_ranking_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_label VARCHAR(20) NOT NULL UNIQUE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_topics INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_snapshot_period (period_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE topic_ranking_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_id BIGINT NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    rank_position INT NOT NULL,
    trend_score DOUBLE DEFAULT 0.0,
    paper_count INT DEFAULT 0,
    citation_count INT DEFAULT 0,
    growth_rate DOUBLE DEFAULT 0.0,
    status VARCHAR(20),
    previous_rank INT,
    rank_change INT,
    CONSTRAINT fk_entry_snapshot FOREIGN KEY (snapshot_id) REFERENCES topic_ranking_snapshots(id) ON DELETE CASCADE,
    INDEX idx_entry_snapshot (snapshot_id),
    INDEX idx_entry_keyword (keyword),
    INDEX idx_entry_rank (rank_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
