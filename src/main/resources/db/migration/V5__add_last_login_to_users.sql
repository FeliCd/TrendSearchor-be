-- V5__add_last_login_to_users.sql
-- Add last_login column to existing users table

ALTER TABLE users ADD COLUMN last_login DATETIME NULL;
