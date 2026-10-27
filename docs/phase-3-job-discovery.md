# CareerPilot AI — Phase 3: Job Discovery and Explainable Matching

## 1. Overview & Objective

Phase 3 delivers an end-to-end Job Discovery and Personalized, Explainable Job Matching subsystem for CareerPilot AI.

Key capabilities delivered:
- **Searchable Job Catalog**: Paginated search, faceted filtering (title, company, keywords, location, employment type, remote arrangement, experience level, skill tag), and multiple sort options (`postedAt`, `title`, `company`, `salaryMax`).
- **Explainable Skill Matching Engine**: A deterministic algorithm calculating required skill compatibility percentages between a user's registered competencies and job requisites, providing transparent lists of matched and missing skills with natural language explanations.
- **Saved Jobs & Bookmarking**: Idempotent bookmarking (`POST /api/jobs/{id}/save`, `DELETE /api/jobs/{id}/save`, `GET /api/jobs/saved`) enforcing authenticated ownership and PostgreSQL Row Level Security (RLS).
- **Personalized Recommendations**: `GET /api/jobs/recommendations` ranking opportunities according to user skill compatibility and highlighting high-match openings.
- **Skills Inventory Management**: Users can manage their technical competencies directly on their profile (`/profile`) and immediately observe personalized match scores and tailored recommendations across `/jobs`.

---

## 2. Database Schema & Migrations

### 2.1 Migration: `V3__phase3_jobs_and_saved_jobs.sql`
Location: `backend/src/main/resources/db/migration/V3__phase3_jobs_and_saved_jobs.sql`

This versioned, non-destructive migration extends the existing Phase 1 database:

1. **Extended `jobs` Table**:
   - `remote_type` (VARCHAR 32, e.g., `'REMOTE'`, `'HYBRID'`, `'ONSITE'`)
   - `experience_level` (VARCHAR 32, e.g., `'ENTRY'`, `'MID'`, `'SENIOR'`, `'LEAD'`)
   - `salary_min` (NUMERIC 12,2)
   - `salary_max` (NUMERIC 12,2)
   - `salary_currency` (VARCHAR 8, default `'USD'`)
   - `posted_at` (TIMESTAMPTZ, default `NOW()`)
   - `updated_at` (TIMESTAMPTZ, default `NOW()`)
   - `is_active` (BOOLEAN, default `TRUE`)
   - `is_sample` (BOOLEAN, default `FALSE`) — distinctly identifies development seed vacancies.

2. **Normalized `job_skills` Table**:
   - `job_id` (UUID FK referencing `jobs(id)` ON DELETE CASCADE)
   - `skill_id` (UUID FK referencing `skills(id)` ON DELETE CASCADE)
   - `is_required` (BOOLEAN NOT NULL DEFAULT TRUE) — differentiates mandatory skills from nice-to-have / optional skills.
   - Primary Key: `(job_id, skill_id)`
   - Indexes: `idx_job_skills_skill` on `(skill_id)` and `idx_job_skills_job` on `(job_id)`.

3. **`saved_jobs` Table**:
   - `user_id` (UUID FK referencing `profiles(user_id)` ON DELETE CASCADE)
   - `job_id` (UUID FK referencing `jobs(id)` ON DELETE CASCADE)
   - `created_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW())
   - Primary Key: `(user_id, job_id)`
   - Indexes: `idx_saved_jobs_user` on `(user_id)` and `idx_saved_jobs_job` on `(job_id)`.

4. **Row Level Security (RLS)**:
   - Enabled on `saved_jobs` so authenticated users can only view and manage their own bookmarks:
     ```sql
     CREATE POLICY saved_jobs_user_isolation ON saved_jobs
       FOR ALL TO authenticated
       USING (auth.uid() = user_id)
       WITH CHECK (auth.uid() = user_id);
     ```

### 2.2 How Migrations Are Applied
- **Flyway Database Migration**: When running against a live PostgreSQL database, Flyway executes `V1__init.sql`, `V2__seed.sql`, and `V3__phase3_jobs_and_saved_jobs.sql` sequentially on startup.
- **H2 In-Memory Testing**: Flyway runs automatically during `mvn test` against H2 in PostgreSQL compatibility mode.
- **Manual Execution**: The SQL script can also be executed directly in the Supabase SQL Editor if Flyway is disabled in production.

---

## 3. Explainable Job Matching Engine

### 3.1 Deterministic Matching Algorithm
The matching logic is encapsulated in `com.careerpilot.service.JobMatchingService` and `SkillNormalizer`.

1. **Normalization & Canonical Aliasing**:
   Skills are trimmed, lowercased, and mapped to canonical tech identifiers.
   | Input Skill / Alias | Canonical Skill |
   | :--- | :--- |
   | `js`, `javascript`, `react.js` | `javascript`, `react` |
   | `ts`, `typescript` | `typescript` |
   | `py`, `python3` | `python` |
   | `k8s`, `kube` | `kubernetes` |
   | `postgres`, `psql` | `postgresql` |
   | `aws`, `amazon web services` | `aws` |
   | `gcp`, `google cloud` | `gcp` |
   | `docker`, `docker container` | `docker` |
   | `node`, `nodejs` | `node.js` |
   | `spring`, `spring boot` | `spring boot` |

2. **Match Formula**:
   $$\text{Match Percentage} = \text{round}\left( \frac{\text{Number of Matched Required Skills}}{\text{Total Required Skills}} \times 100 \right)$$

3. **Edge Case Handling**:
   - **No Required Skills Specified**: If a job specifies only optional skills or zero requirements, `hasRequiredSkills` is set to `false`, `matchPercentage` is set to `null`, and the explanation clearly states: `"This posting has no required skills specified. Matching score is not applicable."`
   - **Empty User Skills Profile**: If an authenticated user has zero skills in their profile, `matchPercentage` is `0`, all required skills are categorized as `missingSkills`, and the explanation returns: `"You haven't listed any skills yet. Add your competencies in your Profile to calculate match accuracy."`
   - **100% Match**: Returns a congratulatory explanation: `"Outstanding fit: you possess all X required skills for this role."`
   - **Partial Match**: Returns transparent details: `"Strong candidate: you match X of Y required skills (Z%). Consider gaining proficiency in: [Missing Skill]."`

4. **Performance & N+1 Query Prevention**:
   - `JobRepository.findAllActiveWithSkills()` uses `@EntityGraph(attributePaths = {"jobSkills", "jobSkills.skill"})` to fetch jobs, relationships, and skill names in a single query.
   - `SavedJobRepository.findSavedJobIdsByUserId(userId)` performs a fast bulk fetch of user bookmarks into an in-memory set ($O(1)$ lookups).

---

## 4. REST API Specification

### 4.1 `GET /api/jobs`
Search and filter jobs with pagination and sort options. Publicly accessible; enriches match score and `isSaved` if Bearer token is provided.

**Query Parameters**:
- `query` (optional string): Case-insensitive match on title, company, or description.
- `location` (optional string): Case-insensitive location filter.
- `employmentType` (optional string): `FULL_TIME`, `PART_TIME`, `CONTRACT`, `INTERNSHIP`.
- `remoteType` (optional string): `REMOTE`, `HYBRID`, `ONSITE`.
- `experienceLevel` (optional string): `ENTRY`, `MID`, `SENIOR`, `LEAD`.
- `skill` (optional string): Filter jobs that require or prefer this specific skill.
- `page` (optional int, default: `0`)
- `size` (optional int, default: `10`, max: `50`)
- `sort` (optional string, default: `'postedAt,desc'`)

**Response (`PageResponse<JobSummaryResponse>`)**:
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "title": "[Sample] Senior Full-Stack Engineer",
      "company": "CloudScale Systems",
      "location": "San Francisco, CA",
      "remoteType": "REMOTE",
      "employmentType": "FULL_TIME",
      "experienceLevel": "SENIOR",
      "salaryMin": 140000.0,
      "salaryMax": 185000.0,
      "salaryCurrency": "USD",
      "sourceUrl": "https://example.com/careers/cloudscale/senior-fullstack",
      "isSample": true,
      "postedAt": "2026-10-26T10:00:00Z",
      "requiredSkills": ["Java", "Spring Boot", "React", "TypeScript", "PostgreSQL"],
      "optionalSkills": ["Docker", "Kubernetes"],
      "isSaved": false,
      "match": {
        "jobId": "550e8400-e29b-41d4-a716-446655440001",
        "matchPercentage": 80,
        "matchedRequiredSkills": ["Java", "Spring Boot", "React", "PostgreSQL"],
        "missingRequiredSkills": ["TypeScript"],
        "matchedOptionalSkills": ["Docker"],
        "missingOptionalSkills": ["Kubernetes"],
        "totalRequiredSkills": 5,
        "totalMatchedSkills": 4,
        "explanation": "Strong candidate: you match 4 of 5 required skills (80%). Missing required skills: TypeScript.",
        "hasRequiredSkills": true
      }
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 6,
  "totalPages": 1,
  "last": true
}
```

### 4.2 `GET /api/jobs/{id}`
Retrieve complete job details including formatted description, requirements, and match breakdown.

### 4.3 `GET /api/jobs/saved`
Retrieves all bookmarked vacancies for the authenticated user.
- **Headers**: `Authorization: Bearer <jwt_token>`
- **Response**: Array of `JobSummaryResponse`.

### 4.4 `POST /api/jobs/{id}/save`
Saves/bookmarks a vacancy for the authenticated user.
- **Headers**: `Authorization: Bearer <jwt_token>`
- **Idempotent**: Returns `200 OK` whether newly bookmarked or already bookmarked.
- **Response**:
  ```json
  { "jobId": "...", "saved": true, "message": "Job saved successfully" }
  ```

### 4.5 `DELETE /api/jobs/{id}/save`
Removes a saved vacancy. Idempotent (`200 OK`).

### 4.6 `GET /api/jobs/recommendations`
Returns vacancies ranked in descending order of match percentage.
- **Headers**: `Authorization: Bearer <jwt_token>`
- **Empty State**: If user has no skills, returns an empty list with prompt to add skills.

### 4.7 `GET /api/jobs/{id}/match`
Returns the explainable match calculation for a single job against the authenticated user.

### 4.8 `GET /api/skills` & `POST /api/profile/skills`
- `GET /api/skills`: Returns global skill taxonomy for autosuggestion / dropdowns.
- `GET /api/profile/skills`: Returns authenticated user's current skills inventory.
- `POST /api/profile/skills`: Adds a skill to the user's profile (`skillId` or new skill `name`, `proficiencyLevel`, `yearsExperience`).
- `DELETE /api/profile/skills/{skillId}`: Removes a skill from the user's profile.

---

## 5. Development Seed Data

To facilitate local development, realistic sample jobs are provided:
- **Location**: `backend/src/main/resources/db/seed/dev_seed.sql`
- **Automatic Ingestion**: `com.careerpilot.config.JobDataSeeder` runs on backend startup. If the `jobs` repository is empty, it automatically populates:
  - 20 industry standard skills (Java, Spring Boot, React, TypeScript, Python, Go, Docker, Kubernetes, AWS, PostgreSQL, etc.).
  - 6 sample positions spanning Full-Stack, Frontend, DevOps, ML Platform, Backend (Go), and Data Engineering.
  - All sample listings carry `is_sample = true` and `[Sample]` title prefixes for strict transparency.
- **Manual Ingestion**:
  ```bash
  psql -U postgres -d careerpilot -f backend/src/main/resources/db/seed/dev_seed.sql
  ```

---

## 6. Frontend Implementation

### 6.1 Job Discovery View (`/jobs`)
File: `frontend/src/pages/jobs/JobsPage.tsx`
- **Tab Navigation**:
  - `Explore All Openings`: Paginated search with multi-filter drawer (Remote type, Experience level, Employment type).
  - `Recommended For You`: Personalized opportunities ranked by skill compatibility.
  - `Saved Jobs`: Dedicated bookmarks list.
- **Job Card Highlights**:
  - Title, company, location, badges (remote, full-time, experience).
  - Compensation pills (`$140k - $185k/yr`).
  - Color-coded match badge: green for $\ge 70\%$, amber for $40-69\%$, neutral for $< 40\%$.
  - Single-click bookmark toggle with immediate visual feedback.
  - "View Details & Match" opening the modal.

### 6.2 Job Detail & Explainable Match Modal
File: `frontend/src/components/jobs/JobDetailModal.tsx`
- Complete formatted job description.
- **Match Breakdown Card**:
  - Animated progress bar showing match percentage.
  - List of matched required skills (green tags with checkmarks).
  - List of missing required skills (amber tags with warning icons).
  - Optional skill fit analysis.
  - Plain-language explanation paragraph.
- Action buttons: "Apply on Company Site" (safe external link with `noopener noreferrer`) and "Save Job".

### 6.3 Technical Skills Inventory (`/profile`)
File: `frontend/src/pages/ProfilePage.tsx`
- Interactive competency manager:
  - View all registered skills with proficiency badges and years of experience.
  - Add new skills with category, proficiency level (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`), and years of experience.
  - Delete skills with instant reactive update to `/jobs` match scores.

### 6.4 Dashboard Integration (`/dashboard`)
File: `frontend/src/pages/DashboardPage.tsx`
- Wired real `savedJobsCount` from `GET /api/jobs/saved`.
- Quick link navigates directly to `/jobs?tab=saved`.

---

## 7. Verification & Testing

### 7.1 Backend Unit & Integration Tests
Executed via `mvn test`:
- `SkillNormalizerTest`: Validates whitespace stripping, lowercase transformations, and canonical alias mapping.
- `JobMatchingServiceTest`: Validates 100% match, partial match, alias resolution, 0-skill profile, and 0-required-skills edge cases.
- `JobServiceTest`: Validates search, filters, pagination, saved job toggles, and ownership enforcement.
- `JobControllerTest`: Validates Spring Security rules, public browsing vs authenticated endpoints, and 401/404 handling.
- **Result**: **42 / 42 tests passing** (0 failures, 0 errors).

### 7.2 Frontend Production Build
Executed via `npm run build`:
- Validated TypeScript types across all DTOs and API clients.
- Clean compilation to `dist/` with 0 warnings/errors.
