-- ==============================================================================
-- CareerPilot AI — Development Seed Data (Strictly Development / Local Testing Only)
-- dev_seed.sql
-- DO NOT RUN IN PRODUCTION ENVIRONMENTS
-- ==============================================================================

-- 1. Seed Comprehensive Tech Skills Catalog
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
    ('00000000-0000-0000-0000-000000000010', 'System Architecture', 'Architecture'),
    ('00000000-0000-0000-0000-000000000011', 'Python', 'Backend'),
    ('00000000-0000-0000-0000-000000000012', 'Kubernetes', 'DevOps'),
    ('00000000-0000-0000-0000-000000000013', 'Node.js', 'Backend'),
    ('00000000-0000-0000-0000-000000000014', 'Go', 'Backend'),
    ('00000000-0000-0000-0000-000000000015', 'GraphQL', 'API'),
    ('00000000-0000-0000-0000-000000000016', 'Next.js', 'Frontend'),
    ('00000000-0000-0000-0000-000000000017', 'Redis', 'Database'),
    ('00000000-0000-0000-0000-000000000018', 'Linux', 'Infrastructure'),
    ('00000000-0000-0000-0000-000000000019', 'CI/CD', 'DevOps'),
    ('00000000-0000-0000-0000-000000000020', 'Microservices', 'Architecture')
ON CONFLICT (name) DO NOTHING;

-- 2. Seed Sample Industry Job Postings (Clearly Labeled as Samples)
INSERT INTO jobs (
    id, title, company, location, description, employment_type, remote_type, 
    experience_level, salary_min, salary_max, salary_currency, source_url, 
    posted_at, is_active, is_sample
) VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'Senior Full-Stack Engineer',
        'Acme Cloud Systems [Sample]',
        'San Francisco, CA',
        'We are seeking a seasoned Senior Full-Stack Engineer to design and scale enterprise multi-tenant cloud platforms. You will work across distributed Java/Spring Boot microservices and responsive React/TypeScript frontends.',
        'FULL_TIME',
        'REMOTE',
        'SENIOR',
        145000.00,
        185000.00,
        'USD',
        'https://example.com/careers/sample-senior-fullstack',
        CURRENT_TIMESTAMP - INTERVAL '2 days',
        true,
        true
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'Lead Platform Architect',
        'Nova AI Labs [Sample]',
        'San Francisco, CA',
        'Join Nova AI Labs to lead our core compute and orchestration platform. Responsible for scalable event-driven architectures, container mesh deployment on Kubernetes, and high-throughput data pipelines.',
        'FULL_TIME',
        'HYBRID',
        'LEAD',
        190000.00,
        240000.00,
        'USD',
        'https://example.com/careers/sample-lead-platform-architect',
        CURRENT_TIMESTAMP - INTERVAL '4 days',
        true,
        true
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'Frontend UI/UX Engineer',
        'PixelCraft Studio [Sample]',
        'New York, NY',
        'Craft delightful, accessible user experiences using modern React, TypeScript, and Tailwind CSS. Collaborate closely with product designers to ship responsive, high-performance web applications.',
        'FULL_TIME',
        'REMOTE',
        'MID_LEVEL',
        110000.00,
        140000.00,
        'USD',
        'https://example.com/careers/sample-frontend-engineer',
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        true,
        true
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        'Backend Systems Engineer',
        'FinFlow Technologies [Sample]',
        'Austin, TX',
        'Develop transactional banking gateways, resilient REST APIs, and automated ledger services. Strong background in Java, Spring Boot, relational databases, and containerized Docker environments required.',
        'FULL_TIME',
        'HYBRID',
        'SENIOR',
        150000.00,
        190000.00,
        'USD',
        'https://example.com/careers/sample-backend-engineer',
        CURRENT_TIMESTAMP - INTERVAL '3 days',
        true,
        true
    ),
    (
        '10000000-0000-0000-0000-000000000005',
        'Cloud DevOps & Reliability Lead',
        'Apex Infrastructure [Sample]',
        'Seattle, WA',
        'Maintain 99.99% uptime for global microservices workloads. Expertise in Kubernetes cluster administration, Terraform, Linux systems, and comprehensive CI/CD deployment automation.',
        'FULL_TIME',
        'ONSITE',
        'LEAD',
        160000.00,
        210000.00,
        'USD',
        'https://example.com/careers/sample-devops-lead',
        CURRENT_TIMESTAMP - INTERVAL '5 days',
        true,
        true
    ),
    (
        '10000000-0000-0000-0000-000000000006',
        'Software Engineering Exploratory Intern',
        'InnovateX Labs [Sample]',
        'Remote',
        'An open-ended summer internship position for passionate technologists curious about software development, automated testing, and product experimentation. No rigid skill prerequisites required.',
        'INTERNSHIP',
        'REMOTE',
        'ENTRY_LEVEL',
        50000.00,
        70000.00,
        'USD',
        'https://example.com/careers/sample-internship',
        CURRENT_TIMESTAMP - INTERVAL '6 days',
        true,
        true
    )
ON CONFLICT (id) DO UPDATE SET
    remote_type = EXCLUDED.remote_type,
    experience_level = EXCLUDED.experience_level,
    salary_min = EXCLUDED.salary_min,
    salary_max = EXCLUDED.salary_max,
    salary_currency = EXCLUDED.salary_currency,
    is_sample = EXCLUDED.is_sample;

-- 3. Seed Job Skills (Normalized Required & Optional Skills)
-- Job 1: Senior Full-Stack Engineer
-- Required: Java, Spring Boot, React, TypeScript | Optional: Docker, AWS
INSERT INTO job_skills (job_id, skill_id, is_required) VALUES
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', true),  -- Java
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', true),  -- Spring Boot
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', true),  -- TypeScript
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000004', true),  -- React
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000007', false), -- Docker
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000008', false)  -- AWS
ON CONFLICT (job_id, skill_id) DO NOTHING;

-- Job 2: Lead Platform Architect
-- Required: System Architecture, Docker, AWS, Kubernetes | Optional: Java, REST API Design
INSERT INTO job_skills (job_id, skill_id, is_required) VALUES
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000010', true),  -- System Architecture
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000007', true),  -- Docker
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000008', true),  -- AWS
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000012', true),  -- Kubernetes
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', false), -- Java
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000009', false)  -- REST API Design
ON CONFLICT (job_id, skill_id) DO NOTHING;

-- Job 3: Frontend UI/UX Engineer
-- Required: React, TypeScript, Tailwind CSS | Optional: Next.js, GraphQL
INSERT INTO job_skills (job_id, skill_id, is_required) VALUES
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000004', true),  -- React
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', true),  -- TypeScript
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000006', true),  -- Tailwind CSS
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000016', false), -- Next.js
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000015', false)  -- GraphQL
ON CONFLICT (job_id, skill_id) DO NOTHING;

-- Job 4: Backend Systems Engineer
-- Required: Java, Spring Boot, PostgreSQL, Docker | Optional: Redis, REST API Design
INSERT INTO job_skills (job_id, skill_id, is_required) VALUES
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', true),  -- Java
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000002', true),  -- Spring Boot
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000005', true),  -- PostgreSQL
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000007', true),  -- Docker
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000017', false), -- Redis
    ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000009', false)  -- REST API Design
ON CONFLICT (job_id, skill_id) DO NOTHING;

-- Job 5: Cloud DevOps & Reliability Lead
-- Required: Docker, Kubernetes, AWS, Linux | Optional: CI/CD, System Architecture
INSERT INTO job_skills (job_id, skill_id, is_required) VALUES
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000007', true),  -- Docker
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000012', true),  -- Kubernetes
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000008', true),  -- AWS
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000018', true),  -- Linux
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000019', false), -- CI/CD
    ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000010', false)  -- System Architecture
ON CONFLICT (job_id, skill_id) DO NOTHING;

-- Note: Job 6 (Software Engineering Exploratory Intern) has zero job_skills entries
-- to test and verify deterministic handling of jobs with no specified required skills.
