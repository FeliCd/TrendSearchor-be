CREATE TABLE paper_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    paper_external_id VARCHAR(255) NOT NULL,
    content TEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT uk_paper_notes_user_paper UNIQUE (user_id, paper_external_id),
    CONSTRAINT fk_paper_notes_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
