-- V3__add_builtin_column.sql
-- Add builtin flag to prevent deletion of system accounts

ALTER TABLE users ADD COLUMN builtin BOOLEAN NOT NULL DEFAULT FALSE;
