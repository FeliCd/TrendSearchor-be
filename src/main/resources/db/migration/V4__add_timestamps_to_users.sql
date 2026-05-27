-- V1__add_timestamps_to_users.sql
-- Add created_at and updated_at columns to existing users table

ALTER TABLE users ADD COLUMN created_at DATETIME NULL;
ALTER TABLE users ADD COLUMN updated_at DATETIME NULL;

-- Backfill existing rows
UPDATE users SET created_at = NOW() WHERE created_at IS NULL;
UPDATE users SET updated_at = NOW() WHERE updated_at IS NULL;
