-- ==============================================================================
-- CareerPilot AI — Phase 3 Database Migration
-- V3__phase3_jobs_and_saved_jobs.sql
-- ==============================================================================

-- 1. Extend Jobs Catalog with Phase 3 attributes
ALTER TABLE jobs 
    ADD COLUMN IF NOT EXISTS remote_type VARCHAR(50) CONSTRAINT chk_remote_type CHECK (
        remote_type IS NULL OR remote_type IN ('REMOTE', 'HYBRID', 'ONSITE')
    ),
    ADD COLUMN IF NOT EXISTS experience_level VARCHAR(50) CONSTRAINT chk_experience_level CHECK (
        experience_level IS NULL OR experience_level IN ('ENTRY_LEVEL', 'MID_LEVEL', 'SENIOR', 'LEAD', 'EXECUTIVE')
    ),
    ADD COLUMN IF NOT EXISTS salary_min NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS salary_max NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS salary_currency VARCHAR(10) DEFAULT 'USD',
    ADD COLUMN IF NOT EXISTS posted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_jobs_active ON jobs(is_active);
CREATE INDEX IF NOT EXISTS idx_jobs_posted_at ON jobs(posted_at DESC);
CREATE INDEX IF NOT EXISTS idx_jobs_remote_type ON jobs(remote_type);
CREATE INDEX IF NOT EXISTS idx_jobs_experience_level ON jobs(experience_level);
CREATE INDEX IF NOT EXISTS idx_jobs_location ON jobs(location);
CREATE INDEX IF NOT EXISTS idx_jobs_employment_type ON jobs(employment_type);

-- 2. Job Skills Normalized Relationship
CREATE TABLE IF NOT EXISTS job_skills (
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    is_required BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (job_id, skill_id)
);

CREATE INDEX IF NOT EXISTS idx_job_skills_job ON job_skills(job_id);
CREATE INDEX IF NOT EXISTS idx_job_skills_skill ON job_skills(skill_id);
CREATE INDEX IF NOT EXISTS idx_job_skills_required ON job_skills(is_required);

-- 3. Saved Jobs Table (User bookmarks)
CREATE TABLE IF NOT EXISTS saved_jobs (
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, job_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_jobs_user ON saved_jobs(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_jobs_job ON saved_jobs(job_id);

-- 4. Row Level Security Policies for New Tables
ALTER TABLE job_skills ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view job skills"
    ON job_skills FOR SELECT
    TO authenticated
    USING (true);

ALTER TABLE saved_jobs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own saved jobs"
    ON saved_jobs FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own saved jobs"
    ON saved_jobs FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own saved jobs"
    ON saved_jobs FOR DELETE
    USING (auth.uid() = user_id);
