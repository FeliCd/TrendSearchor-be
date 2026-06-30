-- V9__rename_username_to_full_name.sql
-- Drop the unique constraint and index on username
ALTER TABLE users DROP INDEX uk_users_username;
ALTER TABLE users DROP INDEX idx_users_username;

-- Rename username to full_name and change length from 50 to 200
ALTER TABLE users CHANGE username full_name VARCHAR(200);
