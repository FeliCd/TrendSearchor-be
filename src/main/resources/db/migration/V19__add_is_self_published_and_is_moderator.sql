-- V19: Add is_self_published to research_papers and is_moderator to users

ALTER TABLE users ADD COLUMN is_moderator BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE research_papers ADD COLUMN is_self_published BOOLEAN NOT NULL DEFAULT FALSE;
