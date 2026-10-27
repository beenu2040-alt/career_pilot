# CareerPilot AI — Phase 4: AI Resume Analyzer

## 1. Overview & Objective

Phase 4 delivers an end-to-end, privacy-centric AI Resume Analyzer for CareerPilot AI. Authenticated users can upload resume documents in PDF format, extract and validate structured text content, run deterministic and LLM-powered evaluations against a transparent 0–100 rubric, and receive actionable bullet-point rewrites and job-tailored fit analyses.

Key capabilities delivered:
- **Defensive Document Ingestion**: Strict validation verifying PDF magic bytes (`%PDF-`), MIME type, file size (up to 10MB), non-encryption, and OCR readability (minimum 50 extracted characters).
- **Private Storage Isolation**: Resumes stored under `{userId}/{resumeId}.pdf` in Supabase Storage with local filesystem fallback for offline/development environments.
- **Explainable 0–100 Scoring Rubric**: Transparent diagnostic breakdown evaluating Skills Evidence (25%), Experience & Impact (30%), Clarity & Structure (25%), and Accomplishment Specificity (20%).
- **Multi-Provider AI Architecture**: Configurable integration supporting Google Gemini (`gemini-1.5-flash`), OpenAI-compatible models, and a deterministic offline fallback analyzer.
- **Target Role Audit**: Compares resume text against active job requisitions from Phase 3, identifying verified competencies, missing required skills, and tailored resume bullet rewrites.
- **Privacy & Cascade Deletion**: Deleting a resume automatically purges the stored PDF artifact and cascades deletion to all associated analysis records.

---

## 2. Database Schema & Migration

### Migration: `V4__phase4_resumes_and_analyses.sql`
Location: `backend/src/main/resources/db/migration/V4__phase4_resumes_and_analyses.sql`

### 2.1 Table: `resumes`
Stores uploaded resume file metadata and processing state:
| Column | Type | Constraints / Description |
| :--- | :--- | :--- |
| `id` | `UUID` | Primary Key (`gen_random_uuid()`) |
| `user_id` | `UUID` | Foreign Key &rarr; `profiles(user_id)` ON DELETE CASCADE |
| `original_filename` | `VARCHAR(255)` | User's original file name |
| `storage_path` | `VARCHAR(512)` | Internal object path (`supabase://...` or `local://...`) |
| `file_size` | `BIGINT` | File size in bytes |
| `mime_type` | `VARCHAR(100)` | Verified MIME type (`application/pdf`) |
| `processing_status` | `VARCHAR(50)` | `UPLOADED`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `created_at` | `TIMESTAMPTZ` | Timestamp of upload |
| `updated_at` | `TIMESTAMPTZ` | Timestamp of last modification |

### 2.2 Table: `resume_analyses`
Stores structured audit results, scores, and recommendations:
| Column | Type | Constraints / Description |
| :--- | :--- | :--- |
| `id` | `UUID` | Primary Key (`gen_random_uuid()`) |
| `resume_id` | `UUID` | Foreign Key &rarr; `resumes(id)` ON DELETE CASCADE |
| `user_id` | `UUID` | Foreign Key &rarr; `profiles(user_id)` ON DELETE CASCADE |
| `job_id` | `UUID` | Optional Foreign Key &rarr; `jobs(id)` ON DELETE SET NULL |
| `analysis_status` | `VARCHAR(50)` | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `overall_score` | `INTEGER` | Rubric score between 0 and 100 |
| `summary` | `TEXT` | Executive candidate summary |
| `dimension_scores` | `JSONB / TEXT` | Detailed breakdown of the 4 rubric dimensions |
| `extracted_data` | `JSONB / TEXT` | Candidate demographics, word count, detected sections |
| `strengths` | `JSONB / TEXT` | Array of demonstrated strengths with quote evidence |
| `improvements` | `JSONB / TEXT` | Prioritized suggestions with Before & After examples |
| `skill_analysis` | `JSONB / TEXT` | Technical skills with evidence vs keyword-only listings |
| `experience_analysis` | `JSONB / TEXT` | Quantifiable metrics count and action verb strength |
| `model_identifier` | `VARCHAR(100)`| Model or engine used (e.g. `gemini:gemini-1.5-flash`) |
| `created_at` | `TIMESTAMPTZ` | Analysis creation timestamp |
| `completed_at` | `TIMESTAMPTZ` | Analysis completion timestamp |

### 2.3 Row Level Security (RLS)
Both tables enforce strict Row Level Security ensuring authenticated users can query, insert, and delete only their own records:
```sql
CREATE POLICY "Users can view own resumes" ON resumes FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can view own resume analyses" ON resume_analyses FOR SELECT USING (auth.uid() = user_id);
```

---

## 3. Storage Architecture & Manual Setup

### 3.1 Supabase Storage Setup (Production)
1. Open the [Supabase Dashboard](https://supabase.com/dashboard) and navigate to **Storage**.
2. Click **New Bucket**:
   - Name: `resumes`
   - Public bucket: **Disabled (Private)**
3. Under **Policies**, ensure service role access or add RLS policies allowing authenticated users to manage objects in their folder:
   ```sql
   CREATE POLICY "Users access own resumes" ON storage.objects
     FOR ALL TO authenticated
     USING (bucket_id = 'resumes' AND (storage.foldername(name))[1] = auth.uid()::text);
   ```

### 3.2 Local Development Storage (Offline Fallback)
If `SUPABASE_URL` or `SUPABASE_SERVICE_ROLE_KEY` are omitted from the environment, the backend automatically activates the `CompositeResumeStorageService` local storage adapter:
- Files are saved to `storage/resumes/{userId}/{resumeId}.pdf`.
- Stored files are gitignored and completely isolated.

---

## 4. Scoring Rubric & Methodology

The resume audit evaluates documents on an explainable 0–100 scale across 4 distinct dimensions:

| Dimension | Max Points | Assessment Type | Evaluation Criteria |
| :--- | :---: | :--- | :--- |
| **Skills Evidence & Relevance** | 25 | Hybrid | Validates technical proficiencies against actual project bullets. Penalizes keyword stuffing where skills lack demonstrated context. |
| **Experience & Impact** | 30 | Deterministic | Counts quantifiable metrics (scale, %, \$, latency, team size) and strong past-tense action verbs. |
| **Clarity & Structure** | 25 | Deterministic | Checks standard section presence (Summary, Experience, Education, Skills, Contact), optimal word count (400–900 words), and ATS readability. |
| **Accomplishment Specificity** | 20 | Model-Assessed | Identifies vague or passive phrasing ("worked on", "assisted") and rewards outcome-oriented achievements. |

### Disclaimer
All evaluations display the following mandatory disclosure:
> *"This score is a diagnostic evaluation generated by CareerPilot AI, not an official industry benchmark or guarantee of employment."*

---

## 5. REST API Endpoints

All resume endpoints require verified Supabase JWT authentication (`Authorization: Bearer <token>`).

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/resumes` | Upload a PDF resume (multipart form-data with `file`). Returns `ResumeSummaryDto`. |
| `GET` | `/api/resumes` | List the authenticated user's uploaded resumes. |
| `GET` | `/api/resumes/{id}` | Retrieve metadata for an owned resume. |
| `DELETE`| `/api/resumes/{id}` | Permanently delete resume, cascading to analyses and storage file. |
| `POST` | `/api/resumes/{id}/analyze` | Initiate analysis. Body accepts optional `{ "jobId": "<uuid>" }`. |
| `GET` | `/api/resumes/{id}/analyses`| Retrieve all analysis iterations for an owned resume. |
| `GET` | `/api/resumes/{id}/analyses/{analysisId}` | Retrieve a specific analysis record by ID. |

---

## 6. Environment Variables Configuration

Add the following to `backend/.env`:
```dotenv
# Supabase Storage Configuration
SUPABASE_URL=https://liivrqhjejphedcmdhkkw.supabase.co
SUPABASE_SERVICE_ROLE_KEY=<your-supabase-service-role-key>
SUPABASE_STORAGE_BUCKET=resumes
RESUME_MAX_FILE_SIZE=10485760

# AI Provider Configuration
# Supported providers: gemini (default), openai, fallback (deterministic engine requiring no key)
AI_PROVIDER=gemini
AI_API_KEY=<your-gemini-or-openai-api-key>
AI_MODEL=gemini-1.5-flash
```

---

## 7. Verification & Build Validation

### Automated Backend Tests
Run the test suite:
```bash
mvn -f backend/pom.xml test
```
All 64 unit, slice, and security tests pass:
- `PdfValidationServiceTest`: Magic bytes verification, MIME types, size limits, encrypted PDF rejection, text normalization.
- `DeterministicResumeAnalysisClientTest`: End-to-end rubric scoring, metrics extraction, evidence tagging, job-specific match comparison.
- `ResumeServiceTest`: Upload, listing, ownership enforcement, delete cascade, storage cleanup.
- `ResumeControllerTest`: 401 unauthenticated enforcement, multipart upload, analysis initiation, ownership isolation.

### Frontend Production Build
```bash
npm --prefix frontend run build
```
Compiled cleanly with Vite and TypeScript:
- Built `dist/assets/index-qk0C2YQf.js` and `index-CPIhUvth.css` with 0 errors.
