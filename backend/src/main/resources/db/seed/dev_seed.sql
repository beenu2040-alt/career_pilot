-- ==============================================================================
-- CareerPilot AI — Development Seed Data (Strictly Development / Local Testing Only)
-- dev_seed.sql
-- DO NOT RUN IN PRODUCTION ENVIRONMENTS
-- ==============================================================================

-- Seed Common Tech Skills
INSERT INTO skills (id, name, category) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Java', 'Backend'),
    ('00000000-0000-0000-0000-000000000002', 'Spring Boot', 'Backend'),
    ('00000000-0000-0000-0000-000000000003', 'TypeScript', 'Frontend'),
    ('00000000-0000-0000-0000-000000000004', 'React', 'Frontend'),
    ('00000000-0000-0000-0000-000000000005', 'PostgreSQL', 'Database'),
    ('00000000-0000-0000-0000-000000000006', 'Tailwind CSS', 'Frontend'),
    ('00000000-0000-0000-0000-000000000007', 'Docker', 'DevOps'),
    ('00000000-0000-0000-0000-000000000008', 'AWS', 'Cloud'),
    ('00000000-0000-0000-0000-000000000009', 'REST API Design', 'Architecture'),
    ('00000000-0000-0000-0000-000000000010', 'System Architecture', 'Architecture')
ON CONFLICT (name) DO NOTHING;

-- Seed Sample Industry Job Postings
INSERT INTO jobs (id, title, company, location, description, employment_type, source_url) VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'Senior Full-Stack Engineer',
        'Acme Cloud Systems',
        'Remote (US/EU)',
        'Building scalable multi-tenant SaaS applications using Java, Spring Boot, and modern React.',
        'REMOTE',
        'https://example.com/careers/senior-fullstack'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'Lead Platform Architect',
        'Nova AI Labs',
        'San Francisco, CA / Hybrid',
        'Architecting event-driven microservices, AI pipeline integrations, and high-throughput data platforms.',
        'HYBRID',
        'https://example.com/careers/lead-platform-architect'
    )
ON CONFLICT (id) DO NOTHING;
