# CareerPilot AI — Phase 7: Application Tracker & Career Analytics

## 1. Overview

Phase 7 introduces an end-to-end **Application Tracker and Career Analytics Platform** designed to manage the complete job search lifecycle. Engineers can track opportunities from both the platform's curated jobs catalog and external postings, monitor interview timelines, manage follow-up reminders, and gain actionable insights from server-side calculated career analytics.

### Key Capabilities
1. **Multi-Stage Kanban Pipeline & List Views**:
   - Visual Kanban board organizing applications across 8 status stages: `WISHLIST`, `APPLIED`, `SCREENING`, `INTERVIEW`, `ASSESSMENT`, `OFFER`, `REJECTED`, and `WITHDRAWN`.
   - Responsive list view with instant search (company, title, notes), status filter chips, and sorting.
2. **Dual Sourcing Model**:
   - **Catalog Jobs**: One-click tracking directly from the Find Jobs search interface and job requisition detail modal, with duplicate application protection for the same user and job.
   - **Manual Applications**: Full support for tracking offline or external requisitions with custom company names, job titles, descriptions, posting URLs, recruiter contacts, and offered compensation.
3. **Application Transition History Audit Log**:
   - Dedicated `application_history` tracking every status change with previous status, new status, transition timestamp, and user audit notes.
   - Chronological timeline visualization within the application detail drawer.
4. **Actionable Follow-up & Interview Reminders**:
   - Server-side reminder engine identifying:
     - **Overdue follow-ups**: Follow-up dates prior to today.
     - **Due today**: Follow-up dates matching today.
     - **Upcoming interviews**: Scheduled interview timestamps in the next 14 days.
     - **Stale applications**: Submitted applications with no activity or follow-up for >14 days.
5. **Deterministic Career Analytics**:
   - Server-side analytics computed directly from the authenticated user's records.
   - Submissions over time timeline charts (using Recharts).
   - Distribution of applications by status stage.
   - **Strict Mathematical Definitions**:
     $$\text{Interview Rate} = \frac{\text{Distinct Applications Reaching Interview / Assessment / Offer Stage}}{\text{Total Submitted Applications}} \times 100$$
     $$\text{Offer Rate} = \frac{\text{Distinct Applications Reaching Offer Stage}}{\text{Total Submitted Applications}} \times 100$$
   - **Mathematical Guardrails**: Wishlist applications are strictly excluded from submitted denominators; zero-denominator cases return `0.0` safely; applications bouncing between interview stages are counted exactly once per metric.

---

## 2. Architecture & Data Flow

```mermaid
flowchart TD
    User([Authenticated User]) --> UI[Applications Page & Jobs Page]
    UI -->|JWT Bearer Token| Security[Spring Security Filter Chain]
    Security --> AuthContext[SecurityUtils.getAuthenticatedUserId]
    
    UI -->|GET /api/applications| AppCtrl[ApplicationController]
    UI -->|POST /api/applications| AppCtrl
    UI -->|PATCH /api/applications/{id}/status| AppCtrl
    UI -->|GET /api/applications/reminders| AppCtrl
    UI -->|GET /api/analytics/applications| AnalyticsCtrl[CareerAnalyticsController]
    
    AppCtrl --> AppService[ApplicationService]
    AnalyticsCtrl --> AnalyticsService[CareerAnalyticsService]
    
    AppService --> AppRepo[(applications table)]
    AppService --> HistoryRepo[(application_history table)]
    AppService --> JobRepo[(jobs catalog table)]
    
    AnalyticsService --> AppRepo
    AnalyticsService --> HistoryRepo
    
    subgraph Supabase PostgreSQL
        AppRepo
        HistoryRepo
        JobRepo
    end
```

---

## 3. Database Schema & Migration

Migration script: `backend/src/main/resources/db/migration/V7__phase7_application_tracking_and_history.sql`

### 3.1 Extended `applications` Table
- `id` (UUID, Primary Key)
- `user_id` (UUID, Foreign Key `profiles(user_id) ON DELETE CASCADE`)
- `job_id` (UUID, Nullable Foreign Key `jobs(id) ON DELETE SET NULL`)
- `company_name` (VARCHAR(255), Not Null)
- `job_title` (VARCHAR(255), Not Null)
- `job_description` (TEXT, Nullable)
- `job_url` (VARCHAR(1024), Nullable)
- `status` (VARCHAR(50), Check constraint: `WISHLIST`, `APPLIED`, `SCREENING`, `INTERVIEW`, `ASSESSMENT`, `OFFER`, `REJECTED`, `WITHDRAWN`)
- `applied_date` (DATE, Nullable)
- `contact_name` (VARCHAR(255), Nullable recruiter/hiring manager name)
- `contact_email` (VARCHAR(255), Nullable recruiter email)
- `salary_offered` (NUMERIC(12,2), Nullable compensation figure)
- `notes` (TEXT, Nullable user notes)
- `next_follow_up_date` (DATE, Nullable reminder date)
- `interview_at` (TIMESTAMPTZ, Nullable scheduled interview date/time)
- `created_at` / `updated_at` (TIMESTAMPTZ, Default `NOW()`)
- **Constraints**:
  - `unique_user_job_application`: UNIQUE(`user_id`, `job_id`) when `job_id IS NOT NULL` to prevent duplicate tracking of catalog vacancies.

### 3.2 `application_history` Table
- `id` (UUID, Primary Key)
- `application_id` (UUID, Foreign Key `applications(id) ON DELETE CASCADE`)
- `user_id` (UUID, Foreign Key `profiles(user_id) ON DELETE CASCADE`)
- `previous_status` (VARCHAR(50), Nullable for initial creation)
- `new_status` (VARCHAR(50), Not Null)
- `notes` (TEXT, Nullable user transition note)
- `changed_at` (TIMESTAMPTZ, Default `NOW()`)
- **Indexes**:
  - `idx_app_history_app_id` on `(application_id)`
  - `idx_app_history_user_id` on `(user_id)`
- **Row Level Security (RLS)**:
  - Enabled with policies enforcing `auth.uid() = user_id`.

---

## 4. Backend REST API Endpoints

All endpoints require a valid Supabase JWT Bearer token in the `Authorization` header. User identity is extracted strictly from the verified security context via `SecurityUtils.getAuthenticatedUserId()`.

| Method | Endpoint | Description | Request Body / Query Params | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/applications` | Paginated search and filter list | `?status=&search=&page=0&size=20&sort=updatedAt,desc` | `200 OK`, `401 Unauthorized` |
| `GET` | `/api/applications/all` | Unpaginated list for Kanban board | None | `200 OK`, `401 Unauthorized` |
| `POST` | `/api/applications` | Create catalog or manual application | `CreateApplicationRequest` JSON | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `GET` | `/api/applications/{id}` | Retrieve application details & history | None | `200 OK`, `403 Forbidden`, `404 Not Found` |
| `PUT` | `/api/applications/{id}` | Update application details | `UpdateApplicationRequest` JSON | `200 OK`, `403 Forbidden`, `404 Not Found` |
| `PATCH` | `/api/applications/{id}/status` | Update status & record history log | `UpdateApplicationStatusRequest` JSON | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `DELETE` | `/api/applications/{id}` | Delete application and history | None | `204 No Content`, `403 Forbidden`, `404 Not Found` |
| `GET` | `/api/applications/reminders` | Retrieve due & upcoming reminders | None | `200 OK`, `401 Unauthorized` |
| `GET` | `/api/analytics/applications` | Retrieve career analytics aggregates | None | `200 OK`, `401 Unauthorized` |

---

## 5. Analytics Metric Formulas & Edge Cases

The `CareerAnalyticsService` calculates all aggregates on the server side:

1. **Total Tracked Applications**:
   - Count of all application records belonging to the authenticated user.
2. **Submitted Applications (Denominator)**:
   - Distinct applications with status $\neq \text{WISHLIST}$ or having at least one submission record.
3. **Interview Count & Interview Rate**:
   - Distinct applications where current status or any historical transition reached `SCREENING`, `INTERVIEW`, `ASSESSMENT`, or `OFFER`.
   - $\text{Rate} = (\text{Interview Count} / \text{Submitted Applications}) \times 100$. If submitted applications $= 0$, returns `0.0`.
4. **Offer Count & Offer Rate**:
   - Distinct applications where current status or any historical transition reached `OFFER`.
   - $\text{Rate} = (\text{Offer Count} / \text{Submitted Applications}) \times 100$. If submitted applications $= 0$, returns `0.0`.
5. **Timeline Submissions Over Time**:
   - Grouped by month (`YYYY-MM`) based on `applied_date` or `created_at`.

---

## 6. Verification & Test Suite

### Backend Tests
All 113 unit and integration tests execute and pass cleanly:
```bash
cd backend
mvn test
```
- `ApplicationServiceTest` (7 tests): Stored job creation, duplicate job rejection (409 Conflict), manual job creation, status transition history auditing, reminder calculations, cross-user ownership isolation, and deletion.
- `CareerAnalyticsServiceTest` (2 tests): Zero applications safe 0.0 rates, distinct deduplication across repeated status transitions, and timeline generation.
- `ApplicationControllerTest` (6 tests): MockMvc tests for unauthenticated rejection, 201 creation, pagination, reminders, status updates, and deletion.
- `CareerAnalyticsControllerTest` (2 tests): MockMvc tests for unauthenticated rejection and JSON analytics retrieval.

### Frontend Typecheck & Build
```bash
cd frontend
npm run build
```
- `tsc && vite build`: Completed with 0 TypeScript errors and 0 bundling issues.
