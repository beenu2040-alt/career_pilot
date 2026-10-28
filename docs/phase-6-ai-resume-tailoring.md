# CareerPilot AI — Phase 6: AI Resume Tailoring

## 1. Overview

Phase 6 introduces **AI Resume Tailoring**, an intelligent system that aligns an engineer's existing resume to specific job postings while adhering to **100% strict factual truth and anti-hallucination policies**.

Engineers can:
1. Select one of their previously uploaded source resumes.
2. Select a target job from the database or enter a job description manually.
3. Review target requirements, ATS keyword coverage, and alignment diagnostics.
4. Generate structured tailoring suggestions for:
   - Professional summary reframing
   - Technical skills reordering and keyword weighting
   - Work experience bullet point enhancements using the Google X-Y-Z formula
   - Project descriptions highlighting target architectural patterns
   - Formatting and ATS parseability improvements
5. Review, edit, accept, or reject individual suggestions.
6. Verify and confirm any suggestions that touch upon tools or metrics not in the source resume (`requiresUserConfirmation`).
7. Save tailored versions without overwriting the original source resume.
8. Preview and download ATS-compliant resume PDFs directly from the application.

---

## 2. Architecture & Data Flow

```mermaid
flowchart TD
    User([Authenticated User]) --> UI[Resume Tailor Page]
    UI -->|JWT Auth Bearer| API[ResumeTailoringController]
    API --> Service[ResumeTailoringService]
    
    Service --> ResumeRepo[(resumes & storage)]
    Service --> JobRepo[(jobs catalog)]
    Service --> PDFValidation[PdfValidationService]
    
    Service --> LLMClient[LlmResumeTailoringClient]
    LLMClient -->|Live AI Provider| Gemini[Gemini / OpenAI API]
    LLMClient -.->|Failover / Config| Fallback[DeterministicResumeTailoringClient]
    
    Fallback --> Suggestions[Tailoring Suggestions & Keyword Coverage]
    Gemini --> Suggestions
    
    UI -->|Review / Edit / Accept| Service
    Service --> VersionRepo[(resume_tailored_versions)]
    
    UI -->|Export PDF| PDFService[PdfResumeExportService (PDFBox 3.0.3)]
    PDFService --> PDFBlob[ATS Compliant PDF Stream]
```

---

## 3. Factual Accuracy and Safety Guardrails

A core requirement of Phase 6 is uncompromising factual accuracy:
- **Strict Anti-Hallucination Prompting**: The system never invents employers, employment dates, degrees, certifications, projects, achievements, metrics, or skills.
- **Untrusted Input Treatment**: Source resumes and job postings are sanitized and treated strictly as untrusted input data, never allowing prompt injection or overriding system instructions.
- **Distinction Between Keywords and Qualifications**:
  - *Missing Keywords*: Concepts, tools, or phrasings related to the candidate's existing work that can be rephrased or emphasized.
  - *Missing Qualifications*: Core credentials, required degrees, or tools where the candidate has no basis in their resume. These are explicitly cataloged under `missingQualifications` and never falsely added.
- **User Confirmation Flagging**: Any suggestion that proposes adding a tool or skill not explicitly evidenced in the source resume is flagged with `requiresUserConfirmation: true` and includes a clear verification prompt (`confirmationPrompt`).
- **No False Guarantees**: Tailoring scores are presented as ATS keyword density and relevance diagnostics, with clear disclaimers that scores do not guarantee hiring or interview outcomes.

---

## 4. Database Schema & Migration

Migration script: `backend/src/main/resources/db/migration/V6__phase6_resume_tailoring.sql`

### 4.1 `resume_tailoring_sessions` Table
- `id` (UUID, Primary Key)
- `user_id` (UUID, Foreign Key `profiles(user_id) ON DELETE CASCADE`)
- `resume_id` (UUID, Foreign Key `resumes(id) ON DELETE CASCADE`)
- `job_id` (UUID, Nullable Foreign Key `jobs(id) ON DELETE SET NULL`)
- `job_title` (VARCHAR(255), Not Null)
- `company_name` (VARCHAR(255), Nullable)
- `job_description_snapshot` (TEXT, Not Null for historical reproducibility)
- `status` (VARCHAR(50): `CREATED`, `GENERATING`, `SUGGESTIONS_READY`, `COMPLETED`, `FAILED`)
- `target_role` (VARCHAR(150))
- `model_identifier` (VARCHAR(100))
- `match_score_before` (INTEGER: 0 to 100)
- `match_score_after` (INTEGER: 0 to 100)
- `keyword_coverage` (JSONB)
- `error_message` (TEXT)
- `created_at` / `updated_at` (TIMESTAMPTZ)

### 4.2 `resume_tailoring_suggestions` Table
- `id` (UUID, Primary Key)
- `session_id` (UUID, Foreign Key `resume_tailoring_sessions(id) ON DELETE CASCADE`)
- `section` (VARCHAR(50): `SUMMARY`, `SKILLS`, `EXPERIENCE`, `PROJECTS`, `FORMATTING`)
- `title` (VARCHAR(255), Not Null)
- `original_content` (TEXT)
- `suggested_content` (TEXT, Not Null)
- `user_edited_content` (TEXT, Nullable)
- `explanation` (TEXT, Not Null, distinguishes keywords vs qualifications)
- `status` (VARCHAR(50): `PENDING`, `ACCEPTED`, `REJECTED`, `EDITED`)
- `requires_user_confirmation` (BOOLEAN, Default false)
- `confirmation_prompt` (TEXT)
- `sequence_order` (INTEGER, Default 1)
- `created_at` / `updated_at` (TIMESTAMPTZ)

### 4.3 `resume_tailored_versions` Table
- `id` (UUID, Primary Key)
- `session_id` (UUID, Foreign Key `resume_tailoring_sessions(id) ON DELETE CASCADE`)
- `user_id` (UUID, Foreign Key `profiles(user_id) ON DELETE CASCADE`)
- `version_number` (INTEGER, Default 1)
- `version_name` (VARCHAR(255), Not Null)
- `tailored_content` (JSONB: structured resume sections)
- `tailored_plain_text` (TEXT: full text export)
- `change_summary` (TEXT)
- `applied_suggestions_count` (INTEGER, Default 0)
- `keyword_coverage` (JSONB)
- `storage_path` (VARCHAR(512))
- `created_at` (TIMESTAMPTZ)

### 4.4 Row Level Security (RLS)
- RLS enabled across all three tables.
- Cross-user ownership verified via `auth.uid() = user_id` and session ownership subqueries.

---

## 5. REST API Endpoints

All endpoints require verified Supabase JWT Bearer token.

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/resume-tailoring` | Create tailoring session | `201 Created` |
| `GET` | `/api/resume-tailoring` | List authenticated user's sessions | `200 OK` |
| `GET` | `/api/resume-tailoring/{id}` | Get session details, suggestions & versions | `200 OK` |
| `POST` | `/api/resume-tailoring/{id}/generate` | Generate LLM tailoring suggestions | `200 OK` |
| `PATCH` | `/api/resume-tailoring/{id}/suggestions/{suggestionId}` | Accept, reject, or edit suggestion | `200 OK` |
| `POST` | `/api/resume-tailoring/{id}/versions` | Save reviewed version | `201 Created` |
| `GET` | `/api/resume-tailoring/{id}/versions/{versionId}` | Retrieve specific tailored version | `200 OK` |
| `GET` | `/api/resume-tailoring/{id}/versions/{versionId}/export` | Export tailored resume as PDF | `200 OK (application/pdf)` |
| `DELETE` | `/api/resume-tailoring/{id}` | Delete session and cascading data | `204 No Content` |

---

## 6. PDF Resume Export (Apache PDFBox 3.0.3)

`PdfResumeExportService` generates clean, professional, ATS-compliant PDF resumes directly from the Spring Boot backend:
- Single-column layout favored by major ATS parsers (Workday, Greenhouse, Lever).
- Professional typography with standard Helvetica Type 1 font metrics.
- Word wrapping, bullet point indentation, and dynamic page-break management.
- Unicode sanitization for resilient text streaming without encoding exceptions.
- Direct binary download via `GET /api/resume-tailoring/{id}/versions/{versionId}/export`.

---

## 7. Configuration & Environment Variables

Add to `backend/.env` (optional for live LLM):
```bash
# AI Configuration (Optional: fallback engine is active by default)
AI_PROVIDER=gemini # or openai, fallback
AI_API_KEY=your-api-key-here
AI_MODEL=gemini-1.5-flash
AI_TIMEOUT_MS=30000
```

---

## 8. Manual Verification Guide

1. **Start Backend**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
2. **Start Frontend**:
   ```bash
   cd frontend
   npm run dev
   ```
3. **Log In**:
   Log in with an authenticated user at `http://localhost:5173/login`.
4. **Ensure Source Resume Exists**:
   Navigate to `/resume-analyzer` and upload a PDF resume if none exists.
5. **Open Resume Tailor**:
   Click **Resume Tailor** in the navigation bar (`/resume-tailor`).
6. **Create Tailoring Session**:
   - Click **New Tailoring Session**.
   - Select your source resume.
   - Choose a target job from the database OR enter job requirements manually.
   - Click **Create & Analyze**.
7. **Review Tailoring Diagnostics**:
   - Inspect the keyword alignment score (Before vs After).
   - Review matched keywords vs missing qualifications.
8. **Review & Edit Suggestions**:
   - Review suggestions across Summary, Skills, Experience, and Projects.
   - Accept or Reject suggestions.
   - Click **Edit** on a suggestion, customize the text, and save.
   - Observe the Factual Confirmation banner for suggestions flagged with `requiresUserConfirmation`.
9. **Save Version**:
   - Click **Save As Version**.
   - Navigate to the **Tailored Preview & Versions** tab.
10. **Export & Download PDF**:
   - Click **Download PDF** to receive the generated `.pdf` resume file.
   - Click **Copy Text** to copy the full plain text resume.
