-- V12__restore_status_columns.sql

ALTER TABLE research_papers
DROP FOREIGN KEY fk_research_papers_submitted_by,
DROP FOREIGN KEY fk_research_papers_validated_by;

ALTER TABLE research_papers
CHANGE COLUMN validation_status status VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
CHANGE COLUMN submitted_by_user_id uploaded_by_id BIGINT NULL,
CHANGE COLUMN validated_by_id approved_by_id BIGINT NULL;

ALTER TABLE research_papers
ADD CONSTRAINT fk_research_papers_uploaded_by FOREIGN KEY (uploaded_by_id) REFERENCES users (id) ON DELETE SET NULL,
ADD CONSTRAINT fk_research_papers_approved_by FOREIGN KEY (approved_by_id) REFERENCES users (id) ON DELETE SET NULL;
