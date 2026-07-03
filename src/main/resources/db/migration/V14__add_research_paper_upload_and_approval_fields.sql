-- V10__add_research_paper_upload_and_approval_fields.sql

ALTER TABLE research_papers
ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
ADD COLUMN uploaded_by_id BIGINT NULL,
ADD COLUMN approved_by_id BIGINT NULL,
ADD COLUMN status_comments TEXT NULL,
ADD CONSTRAINT fk_research_papers_uploaded_by FOREIGN KEY (uploaded_by_id) REFERENCES users (id) ON DELETE SET NULL,
ADD CONSTRAINT fk_research_papers_approved_by FOREIGN KEY (approved_by_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_papers_status ON research_papers (status);
