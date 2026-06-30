-- Add fields to support researcher paper uploads and moderation workflow
ALTER TABLE research_papers ADD COLUMN source VARCHAR(20) DEFAULT 'OPENALEX';
ALTER TABLE research_papers ADD COLUMN upload_status VARCHAR(20) DEFAULT NULL;
ALTER TABLE research_papers ADD COLUMN uploaded_by BIGINT DEFAULT NULL;
ALTER TABLE research_papers ADD COLUMN rejection_reason VARCHAR(500) DEFAULT NULL;
ALTER TABLE research_papers ADD COLUMN pdf_url VARCHAR(500) DEFAULT NULL;

ALTER TABLE research_papers ADD CONSTRAINT fk_paper_uploaded_by
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_papers_upload_status ON research_papers(upload_status);
CREATE INDEX idx_papers_source ON research_papers(source);
CREATE INDEX idx_papers_uploaded_by ON research_papers(uploaded_by);
