-- V6__add_status_to_users.sql
-- Add status column to existing users table

ALTER TABLE users ADD COLUMN status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE';
