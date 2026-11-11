-- Apply this migration before deploying the file-service integration.
ALTER TABLE resume_upload_session
    ADD COLUMN IF NOT EXISTS upload_url TEXT;

ALTER TABLE resume_upload_session
    ADD COLUMN IF NOT EXISTS upload_url_expires_at TIMESTAMP;

-- Drop legacy async-confirm columns if an earlier revision was applied.
ALTER TABLE resume
    DROP COLUMN IF EXISTS storage_status,
    DROP COLUMN IF EXISTS storage_error;

ALTER TABLE resume_upload_session
    DROP COLUMN IF EXISTS confirm_attempts,
    DROP COLUMN IF EXISTS confirm_last_error,
    DROP COLUMN IF EXISTS confirm_completed_at;
