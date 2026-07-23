-- V20: Consolidate the paper-moderation model onto research_papers.status,
--      and treat "moderator" purely as a Role (drop the redundant is_moderator flag).
--
-- Background:
--   * research_papers previously had TWO moderation status fields: `status`
--     (PaperStatus) and `upload_status` (UploadStatus). Public search, upload and
--     admin approval all use `status`, while the moderation dashboard read
--     `upload_status` — which was never populated, so moderators never saw the
--     queue. `upload_status` is now derived from `status` in the entity and the
--     stored column is removed.
--   * users.is_moderator duplicated the MODERATOR role but was never used for
--     authorization. Moderator access is granted via role = 'MODERATOR'.

-- Drop the disconnected upload_status column (and its index).
DROP INDEX idx_papers_upload_status ON research_papers;
ALTER TABLE research_papers DROP COLUMN upload_status;

-- Drop the redundant moderator flag; moderator is represented by role = 'MODERATOR'.
ALTER TABLE users DROP COLUMN is_moderator;
