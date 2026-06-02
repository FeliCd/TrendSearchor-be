-- Add must_change_password flag for forced password change after reset
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Add token_version for session invalidation on password reset
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
