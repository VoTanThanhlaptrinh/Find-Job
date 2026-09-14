-- ==============================================================================
-- MIGRATION SCRIPT: CV Presigned Upload Foundation, Resume Status, Idempotency & Cleanup
--
-- Environment: Production / Staging
-- Target Database: PostgreSQL
-- Note: Project does not use Flyway/Liquibase. This script MUST be run manually
--       by the database administrator on production prior to deploying this release.
--       Development ddl-auto:update is NOT a substitute for production migration.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. Migrate column resume.resume_status (Idempotent)
-- ------------------------------------------------------------------------------
ALTER TABLE resume
ADD COLUMN IF NOT EXISTS resume_status VARCHAR(32);

UPDATE resume
SET resume_status =
    CASE
        WHEN is_analyzed = TRUE THEN 'READY'
        ELSE 'UPLOADED'
    END
WHERE resume_status IS NULL;

ALTER TABLE resume
ALTER COLUMN resume_status SET DEFAULT 'UPLOADED';

ALTER TABLE resume
ALTER COLUMN resume_status SET NOT NULL;

-- ------------------------------------------------------------------------------
-- 2. Create table resume_upload_session (Idempotent)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resume_upload_session (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128),
    request_fingerprint VARCHAR(128),
    original_file_name VARCHAR(255) NOT NULL,
    declared_content_type VARCHAR(128) NOT NULL,
    declared_size BIGINT NOT NULL,
    temp_key VARCHAR(512) NOT NULL,
    permanent_key VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    processing_token VARCHAR(128),
    finalizing_started_at TIMESTAMP WITHOUT TIME ZONE,
    rejection_code VARCHAR(64),
    rejection_detail VARCHAR(512),
    rejected_at TIMESTAMP WITHOUT TIME ZONE,
    actual_size BIGINT,
    actual_content_type VARCHAR(128),
    actual_etag VARCHAR(128),
    cleanup_attempts INT DEFAULT 0 NOT NULL,
    cleanup_last_error VARCHAR(512),
    cleanup_completed_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT DEFAULT 0 NOT NULL,
    resume_id BIGINT,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_resume_upload_session_temp_key UNIQUE (temp_key),
    CONSTRAINT uq_resume_upload_session_permanent_key UNIQUE (permanent_key),
    CONSTRAINT uq_resume_upload_session_resume_id UNIQUE (resume_id)
);

-- ------------------------------------------------------------------------------
-- 3. Idempotent column additions for existing installations
-- ------------------------------------------------------------------------------
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(128);
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS processing_token VARCHAR(128);
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS finalizing_started_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS rejection_code VARCHAR(64);
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS rejection_detail VARCHAR(512);
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS actual_size BIGINT;
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS actual_content_type VARCHAR(128);
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS actual_etag VARCHAR(128);
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS cleanup_attempts INT DEFAULT 0 NOT NULL;
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS cleanup_last_error VARCHAR(512);
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS cleanup_completed_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE resume_upload_session ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- ------------------------------------------------------------------------------
-- 4. Constraints (Idempotent)
-- ------------------------------------------------------------------------------
DO $$
BEGIN
    -- Unique constraint (user_id, idempotency_key)
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_resume_upload_session_user_id_idempotency_key'
    ) THEN
        ALTER TABLE resume_upload_session
        ADD CONSTRAINT uq_resume_upload_session_user_id_idempotency_key
        UNIQUE (user_id, idempotency_key);
    END IF;

    -- Invariant check constraint:
    -- REJECTED / EXPIRED / PENDING / FINALIZING must have resume_id IS NULL;
    -- COMPLETED must have resume_id IS NOT NULL.
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_resume_upload_session_status_resume_id'
    ) THEN
        ALTER TABLE resume_upload_session
        ADD CONSTRAINT chk_resume_upload_session_status_resume_id
        CHECK (
            (status = 'COMPLETED' AND resume_id IS NOT NULL) OR
            (status <> 'COMPLETED' AND resume_id IS NULL)
        );
    END IF;
END $$;

-- ------------------------------------------------------------------------------
-- 5. Indexes (Idempotent)
-- ------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_resume_upload_session_user_id
    ON resume_upload_session (user_id);

CREATE INDEX IF NOT EXISTS idx_resume_upload_session_status_expires
    ON resume_upload_session (status, expires_at);

CREATE INDEX IF NOT EXISTS idx_resume_upload_session_status_finalizing
    ON resume_upload_session (status, finalizing_started_at);
