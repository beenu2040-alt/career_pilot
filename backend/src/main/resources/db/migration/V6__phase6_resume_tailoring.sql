-- ==============================================================================
-- CareerPilot AI — Phase 6 Database Migration
-- V6__phase6_resume_tailoring.sql
-- ==============================================================================

-- 1. Resume Tailoring Sessions Table
CREATE TABLE IF NOT EXISTS resume_tailoring_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    job_id UUID REFERENCES jobs(id) ON DELETE SET NULL,
    job_title VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    job_description_snapshot TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED' CONSTRAINT chk_tailoring_session_status CHECK (
        status IN ('CREATED', 'GENERATING', 'SUGGESTIONS_READY', 'COMPLETED', 'FAILED')
    ),
    target_role VARCHAR(150),
    model_identifier VARCHAR(100),
    match_score_before INTEGER CONSTRAINT chk_score_before CHECK (
        match_score_before IS NULL OR (match_score_before >= 0 AND match_score_before <= 100)
    ),
    match_score_after INTEGER CONSTRAINT chk_score_after CHECK (
        match_score_after IS NULL OR (match_score_after >= 0 AND match_score_after <= 100)
    ),
    keyword_coverage JSONB,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tailoring_sessions_user ON resume_tailoring_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_tailoring_sessions_resume ON resume_tailoring_sessions(resume_id);
CREATE INDEX IF NOT EXISTS idx_tailoring_sessions_job ON resume_tailoring_sessions(job_id);
CREATE INDEX IF NOT EXISTS idx_tailoring_sessions_created_at ON resume_tailoring_sessions(created_at DESC);

-- 2. Resume Tailoring Suggestions Table
CREATE TABLE IF NOT EXISTS resume_tailoring_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES resume_tailoring_sessions(id) ON DELETE CASCADE,
    section VARCHAR(50) NOT NULL CONSTRAINT chk_suggestion_section CHECK (
        section IN ('SUMMARY', 'SKILLS', 'EXPERIENCE', 'PROJECTS', 'FORMATTING')
    ),
    title VARCHAR(255) NOT NULL,
    original_content TEXT,
    suggested_content TEXT NOT NULL,
    user_edited_content TEXT,
    explanation TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CONSTRAINT chk_suggestion_status CHECK (
        status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EDITED')
    ),
    requires_user_confirmation BOOLEAN NOT NULL DEFAULT FALSE,
    confirmation_prompt TEXT,
    sequence_order INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tailoring_suggestions_session ON resume_tailoring_suggestions(session_id);
CREATE INDEX IF NOT EXISTS idx_tailoring_suggestions_order ON resume_tailoring_suggestions(session_id, sequence_order);

-- 3. Resume Tailored Versions Table
CREATE TABLE IF NOT EXISTS resume_tailored_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES resume_tailoring_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL DEFAULT 1,
    version_name VARCHAR(255) NOT NULL,
    tailored_content JSONB NOT NULL,
    tailored_plain_text TEXT NOT NULL,
    change_summary TEXT,
    applied_suggestions_count INTEGER NOT NULL DEFAULT 0,
    keyword_coverage JSONB,
    storage_path VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tailored_versions_session ON resume_tailored_versions(session_id);
CREATE INDEX IF NOT EXISTS idx_tailored_versions_user ON resume_tailored_versions(user_id);
CREATE INDEX IF NOT EXISTS idx_tailored_versions_created_at ON resume_tailored_versions(created_at DESC);

-- 4. Row Level Security Policies
ALTER TABLE resume_tailoring_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own tailoring sessions"
    ON resume_tailoring_sessions FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own tailoring sessions"
    ON resume_tailoring_sessions FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own tailoring sessions"
    ON resume_tailoring_sessions FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own tailoring sessions"
    ON resume_tailoring_sessions FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

ALTER TABLE resume_tailoring_suggestions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own tailoring suggestions"
    ON resume_tailoring_suggestions FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM resume_tailoring_sessions rts
            WHERE rts.id = resume_tailoring_suggestions.session_id
            AND rts.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert own tailoring suggestions"
    ON resume_tailoring_suggestions FOR INSERT
    TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM resume_tailoring_sessions rts
            WHERE rts.id = resume_tailoring_suggestions.session_id
            AND rts.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update own tailoring suggestions"
    ON resume_tailoring_suggestions FOR UPDATE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM resume_tailoring_sessions rts
            WHERE rts.id = resume_tailoring_suggestions.session_id
            AND rts.user_id = auth.uid()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM resume_tailoring_sessions rts
            WHERE rts.id = resume_tailoring_suggestions.session_id
            AND rts.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can delete own tailoring suggestions"
    ON resume_tailoring_suggestions FOR DELETE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM resume_tailoring_sessions rts
            WHERE rts.id = resume_tailoring_suggestions.session_id
            AND rts.user_id = auth.uid()
        )
    );

ALTER TABLE resume_tailored_versions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own tailored versions"
    ON resume_tailored_versions FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own tailored versions"
    ON resume_tailored_versions FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own tailored versions"
    ON resume_tailored_versions FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own tailored versions"
    ON resume_tailored_versions FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);
