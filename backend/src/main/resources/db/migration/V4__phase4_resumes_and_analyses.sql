-- ==============================================================================
-- CareerPilot AI — Phase 4 Database Migration
-- V4__phase4_resumes_and_analyses.sql
-- ==============================================================================

-- 1. Resumes Table
CREATE TABLE IF NOT EXISTS resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    processing_status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED' CONSTRAINT chk_resume_status CHECK (
        processing_status IN ('UPLOADED', 'PROCESSING', 'COMPLETED', 'FAILED')
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resumes_user ON resumes(user_id);
CREATE INDEX IF NOT EXISTS idx_resumes_created_at ON resumes(created_at DESC);

-- 2. Resume Analyses Table
CREATE TABLE IF NOT EXISTS resume_analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    job_id UUID REFERENCES jobs(id) ON DELETE SET NULL,
    analysis_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CONSTRAINT chk_analysis_status CHECK (
        analysis_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    ),
    overall_score INTEGER CONSTRAINT chk_overall_score CHECK (
        overall_score IS NULL OR (overall_score >= 0 AND overall_score <= 100)
    ),
    summary TEXT,
    dimension_scores JSONB,
    extracted_data JSONB,
    strengths JSONB,
    improvements JSONB,
    skill_analysis JSONB,
    experience_analysis JSONB,
    model_identifier VARCHAR(100),
    error_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_resume_analyses_resume ON resume_analyses(resume_id);
CREATE INDEX IF NOT EXISTS idx_resume_analyses_user ON resume_analyses(user_id);
CREATE INDEX IF NOT EXISTS idx_resume_analyses_job ON resume_analyses(job_id);
CREATE INDEX IF NOT EXISTS idx_resume_analyses_created_at ON resume_analyses(created_at DESC);

-- 3. Row Level Security Policies
ALTER TABLE resumes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own resumes"
    ON resumes FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own resumes"
    ON resumes FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own resumes"
    ON resumes FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own resumes"
    ON resumes FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

ALTER TABLE resume_analyses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own resume analyses"
    ON resume_analyses FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own resume analyses"
    ON resume_analyses FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own resume analyses"
    ON resume_analyses FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own resume analyses"
    ON resume_analyses FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);
