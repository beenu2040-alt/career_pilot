-- ==============================================================================
-- CareerPilot AI — Phase 7 Database Migration
-- V7__phase7_application_tracking_and_history.sql
-- ==============================================================================

-- 1. Extend Applications Table for Manual Jobs, Rich Details, and Reminders
ALTER TABLE applications ALTER COLUMN job_id DROP NOT NULL;

ALTER TABLE applications ADD COLUMN IF NOT EXISTS company_name VARCHAR(255);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS job_title VARCHAR(255);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS job_description TEXT;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS job_url VARCHAR(512);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS contact_name VARCHAR(255);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS salary_offered NUMERIC(12, 2);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS next_follow_up_date DATE;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS interview_at TIMESTAMPTZ;

-- Backfill company_name and job_title for existing records from jobs table
UPDATE applications a
SET 
    company_name = j.company,
    job_title = j.title,
    job_description = j.description,
    job_url = j.source_url
FROM jobs j
WHERE a.job_id = j.id AND a.company_name IS NULL;

-- Set default values for any legacy records without company/title
UPDATE applications
SET company_name = 'Unknown Employer'
WHERE company_name IS NULL;

UPDATE applications
SET job_title = 'Target Role'
WHERE job_title IS NULL;

ALTER TABLE applications ALTER COLUMN company_name SET NOT NULL;
ALTER TABLE applications ALTER COLUMN job_title SET NOT NULL;

-- Update status constraint to include full Phase 7 lifecycle
ALTER TABLE applications DROP CONSTRAINT IF EXISTS chk_application_status;
ALTER TABLE applications ADD CONSTRAINT chk_application_status CHECK (
    status IN ('WISHLIST', 'APPLIED', 'SCREENING', 'INTERVIEW', 'ASSESSMENT', 'OFFER', 'REJECTED', 'WITHDRAWN', 'INTERVIEWING')
);

CREATE INDEX IF NOT EXISTS idx_applications_followup ON applications(user_id, next_follow_up_date);
CREATE INDEX IF NOT EXISTS idx_applications_interview ON applications(user_id, interview_at);
CREATE INDEX IF NOT EXISTS idx_applications_applied_at ON applications(user_id, applied_at DESC);

-- 2. Application Status History Table
CREATE TABLE IF NOT EXISTS application_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_app_history_app ON application_history(application_id);
CREATE INDEX IF NOT EXISTS idx_app_history_user ON application_history(user_id);
CREATE INDEX IF NOT EXISTS idx_app_history_created_at ON application_history(created_at DESC);

-- 3. Row Level Security Policies for Application History
ALTER TABLE application_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own application history"
    ON application_history FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own application history"
    ON application_history FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own application history"
    ON application_history FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own application history"
    ON application_history FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);
