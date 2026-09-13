-- ==============================================================================
-- MIGRATION SCRIPT: CV Presigned Upload Foundation & Resume Status
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
    original_file_name VARCHAR(255) NOT NULL,
    declared_content_type VARCHAR(128) NOT NULL,
    declared_size BIGINT NOT NULL,
    temp_key VARCHAR(512) NOT NULL,
    permanent_key VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    resume_id BIGINT,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_resume_upload_session_temp_key UNIQUE (temp_key),
    CONSTRAINT uq_resume_upload_session_permanent_key UNIQUE (permanent_key),
    CONSTRAINT uq_resume_upload_session_resume_id UNIQUE (resume_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_resume_upload_session_user_id
    ON resume_upload_session (user_id);

CREATE INDEX IF NOT EXISTS idx_resume_upload_session_status_expires
    ON resume_upload_session (status, expires_at);
