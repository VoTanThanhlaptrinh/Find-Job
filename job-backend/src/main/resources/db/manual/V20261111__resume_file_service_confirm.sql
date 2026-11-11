-- Apply this migration before deploying the file-service integration.
ALTER TABLE resume
    ADD COLUMN IF NOT EXISTS storage_status VARCHAR(32);

ALTER TABLE resume
    ADD COLUMN IF NOT EXISTS storage_error VARCHAR(512);

UPDATE resume
SET storage_status = 'CONFIRMED'
WHERE storage_status IS NULL;

ALTER TABLE resume
    ALTER COLUMN storage_status SET DEFAULT 'CONFIRMED';

ALTER TABLE resume_upload_session
    ADD COLUMN IF NOT EXISTS upload_url TEXT;

ALTER TABLE resume_upload_session
    ADD COLUMN IF NOT EXISTS upload_url_expires_at TIMESTAMP;

ALTER TABLE resume_upload_session
    ADD COLUMN IF NOT EXISTS confirm_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE resume_upload_session
    ADD COLUMN IF NOT EXISTS confirm_last_error VARCHAR(512);

ALTER TABLE resume_upload_session
    ADD COLUMN IF NOT EXISTS confirm_completed_at TIMESTAMP;
