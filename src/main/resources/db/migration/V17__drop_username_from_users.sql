ALTER TABLE users DROP INDEX idx_users_username;
ALTER TABLE users DROP INDEX uk_users_username;
ALTER TABLE users DROP COLUMN username;
