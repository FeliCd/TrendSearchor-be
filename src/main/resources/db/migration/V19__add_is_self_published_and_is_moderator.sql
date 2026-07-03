-- V13__add_is_self_published_and_is_moderator.sql

ALTER TABLE users ADD COLUMN is_moderator BOOLEAN NOT NULL DEFAULT FALSE;
