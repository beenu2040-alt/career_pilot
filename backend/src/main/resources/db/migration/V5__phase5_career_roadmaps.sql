-- ==============================================================================
-- CareerPilot AI — Phase 5 Database Migration
-- V5__phase5_career_roadmaps.sql
-- ==============================================================================

-- 1. Career Roadmaps Table
CREATE TABLE IF NOT EXISTS career_roadmaps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    target_role VARCHAR(150) NOT NULL,
    current_level VARCHAR(50) DEFAULT 'Mid-Level',
    overall_progress INTEGER NOT NULL DEFAULT 0 CONSTRAINT chk_roadmap_progress CHECK (
        overall_progress >= 0 AND overall_progress <= 100
    ),
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED' CONSTRAINT chk_roadmap_status CHECK (
        status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')
    ),
    summary TEXT,
    skill_gap_summary JSONB,
    model_identifier VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_career_roadmaps_user ON career_roadmaps(user_id);
CREATE INDEX IF NOT EXISTS idx_career_roadmaps_created_at ON career_roadmaps(created_at DESC);

-- 2. Roadmap Milestones Table
CREATE TABLE IF NOT EXISTS roadmap_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    roadmap_id UUID NOT NULL REFERENCES career_roadmaps(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    sequence_order INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED' CONSTRAINT chk_milestone_status CHECK (
        status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')
    ),
    estimated_duration_weeks INTEGER NOT NULL DEFAULT 2,
    completed_at TIMESTAMPTZ,
    skills JSONB,
    projects JSONB,
    resources JSONB,
    completion_criteria JSONB,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_roadmap_milestones_roadmap ON roadmap_milestones(roadmap_id);
CREATE INDEX IF NOT EXISTS idx_roadmap_milestones_order ON roadmap_milestones(roadmap_id, sequence_order);

-- 3. Row Level Security Policies
ALTER TABLE career_roadmaps ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own roadmaps"
    ON career_roadmaps FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own roadmaps"
    ON career_roadmaps FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own roadmaps"
    ON career_roadmaps FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own roadmaps"
    ON career_roadmaps FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

ALTER TABLE roadmap_milestones ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own roadmap milestones"
    ON roadmap_milestones FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM career_roadmaps cr
            WHERE cr.id = roadmap_milestones.roadmap_id
            AND cr.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert own roadmap milestones"
    ON roadmap_milestones FOR INSERT
    TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM career_roadmaps cr
            WHERE cr.id = roadmap_milestones.roadmap_id
            AND cr.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can update own roadmap milestones"
    ON roadmap_milestones FOR UPDATE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM career_roadmaps cr
            WHERE cr.id = roadmap_milestones.roadmap_id
            AND cr.user_id = auth.uid()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM career_roadmaps cr
            WHERE cr.id = roadmap_milestones.roadmap_id
            AND cr.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can delete own roadmap milestones"
    ON roadmap_milestones FOR DELETE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM career_roadmaps cr
            WHERE cr.id = roadmap_milestones.roadmap_id
            AND cr.user_id = auth.uid()
        )
    );
