-- V2__add_notifications_and_synclog.sql
-- Add notification settings for users
ALTER TABLE users ADD COLUMN receive_notifications BOOLEAN DEFAULT TRUE;

-- Add sync schedule for api data sources
ALTER TABLE api_data_sources ADD COLUMN sync_schedule VARCHAR(255) DEFAULT '0 0 2 * * ?';

-- Create sync logs table
CREATE TABLE sync_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id BIGINT NOT NULL,
    sync_start_time DATETIME NOT NULL,
    sync_end_time DATETIME,
    status VARCHAR(255) NOT NULL,
    papers_added INT DEFAULT 0,
    error_message TEXT,
    created_at DATETIME,
    CONSTRAINT fk_sync_logs_source FOREIGN KEY (source_id) REFERENCES api_data_sources (id) ON DELETE CASCADE
);

CREATE INDEX idx_sync_logs_source_created ON sync_logs (source_id, created_at DESC);
