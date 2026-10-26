-- ==============================================================================
-- CareerPilot AI — Phase 1 Row Level Security (RLS) Policies
-- V2__enable_rls_policies.sql
--
-- NOTE: In Supabase, queries executed directly from client libraries (PostgREST)
-- evaluate auth.uid() from the Supabase JWT.
--
-- Spring Boot connects via standard JDBC connection pool. When Spring Boot operates
-- as a trusted backend service, authorization logic is enforced at the application
-- layer (Spring Security + JPA user ownership queries).
-- These policies provide defense-in-depth for any direct Supabase client access.
-- ==============================================================================

-- 1. Profiles RLS
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own profile"
    ON profiles FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own profile"
    ON profiles FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own profile"
    ON profiles FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 2. Skills RLS (Global read-only catalog for authenticated users)
ALTER TABLE skills ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view skills"
    ON skills FOR SELECT
    TO authenticated
    USING (true);

-- 3. User Skills RLS
ALTER TABLE user_skills ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own skills"
    ON user_skills FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own skills"
    ON user_skills FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own skills"
    ON user_skills FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own skills"
    ON user_skills FOR DELETE
    USING (auth.uid() = user_id);

-- 4. Jobs RLS (Global read-only job listings for authenticated users)
ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Authenticated users can view jobs"
    ON jobs FOR SELECT
    TO authenticated
    USING (true);

-- 5. Applications RLS
ALTER TABLE applications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own applications"
    ON applications FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own applications"
    ON applications FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own applications"
    ON applications FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own applications"
    ON applications FOR DELETE
    USING (auth.uid() = user_id);
