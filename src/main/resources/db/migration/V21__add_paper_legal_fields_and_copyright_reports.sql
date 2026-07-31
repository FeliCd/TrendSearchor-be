-- V21: Legal/copyright guardrails for the paper upload feature.
--
-- Background: uploading a paper previously required no confirmation of
-- ownership/authorship, no license selection, and had no takedown mechanism.
-- This migration adds:
--   1. Legal metadata columns on research_papers (license, publication type,
--      ownership/terms confirmation, audit trail, embargo).
--   2. A copyright_reports table for notice-and-takedown.
--   3. TAKEN_DOWN as a valid research_papers.status value (enum is stored as
--      VARCHAR via Hibernate, so no DDL change needed for the enum itself).

ALTER TABLE research_papers ADD COLUMN license VARCHAR(30) NULL;
ALTER TABLE research_papers ADD COLUMN publication_type VARCHAR(30) NULL;
ALTER TABLE research_papers ADD COLUMN ownership_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE research_papers ADD COLUMN terms_accepted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE research_papers ADD COLUMN terms_version VARCHAR(20) NULL;
ALTER TABLE research_papers ADD COLUMN terms_accepted_at DATETIME(6) NULL;
ALTER TABLE research_papers ADD COLUMN uploaded_by_ip VARCHAR(45) NULL;
ALTER TABLE research_papers ADD COLUMN embargo_until DATE NULL;

CREATE TABLE copyright_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paper_id BIGINT NOT NULL,
    reported_by_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolution_notes TEXT NULL,
    reviewed_by_id BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_copyright_reports_paper FOREIGN KEY (paper_id) REFERENCES research_papers (id) ON DELETE CASCADE,
    CONSTRAINT fk_copyright_reports_reported_by FOREIGN KEY (reported_by_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_copyright_reports_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_copyright_reports_paper ON copyright_reports (paper_id);
CREATE INDEX idx_copyright_reports_status ON copyright_reports (status);
