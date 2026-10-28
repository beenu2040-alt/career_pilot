# CareerPilot AI — Career Development Platform

CareerPilot AI is a production-minded career development SaaS application designed to help professionals navigate career transitions, optimize resumes for ATS standards, build customized skill development roadmaps, and track job application pipelines.

This repository contains the completed **Phase 0 (Project Foundation)**, **Phase 1 (Database & Authentication)**, **Phase 2 (Professional UI/UX & Dashboard)**, **Phase 3 (Job Discovery & Explainable Matching)**, **Phase 4 (AI Resume Analyzer)**, **Phase 5 (Personalized Career Roadmap)**, **Phase 6 (AI Resume Tailoring)**, and **Phase 7 (Application Tracker & Career Analytics)** implementations.

---

## 1. Technology Stack

- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui components, React Router, Recharts, `@supabase/supabase-js`
- **Backend**: Java 21 (LTS), Spring Boot 3.3.5 (Spring Web, Spring Validation, Spring Security OAuth2 Resource Server, Spring Data JPA, Flyway, PostgreSQL driver)
- **PDF Generation & Extraction**: Apache PDFBox 3.0.3 (deterministic ATS resume parsing & multi-page PDF generation)
- **AI Integrations**: Gemini API / OpenAI API with deterministic rubric & curriculum fallback engines
- **Database**: Supabase PostgreSQL with version-controlled Flyway migrations & Row Level Security (RLS)
- **Authentication**: Supabase Auth (Email & Password registration, login, session persistence, and password recovery)
- **Backend Security**: Spring Security stateless filter chain with Supabase JWT signature and claims validation
- **Version Control**: Git

---

## 2. Monorepo Structure

```
career_pilot/
├── frontend/                     # React 18 + TypeScript + Vite SPA
│   ├── src/
│   │   ├── components/
│   │   │   ├── auth/             # ProtectedRoute, PublicOnlyRoute
│   │   │   ├── jobs/             # JobDetailModal (with 1-click pipeline tracking)
│   │   │   ├── layout/           # Header, Footer, DashboardLayout
│   │   │   └── ui/               # Button, Card, Badge, Dialog (shadcn/ui)
│   │   ├── context/              # AuthContext (Supabase Auth state & actions)
│   │   ├── lib/                  # supabase client, api client, utils
│   │   ├── pages/
│   │   │   ├── applications/     # ApplicationsPage (Phase 7 Kanban, List & Analytics)
│   │   │   ├── auth/             # LoginPage, RegisterPage, ForgotPassword, ResetPassword
│   │   │   ├── jobs/             # JobsPage (Phase 3)
│   │   │   ├── resume/           # ResumePage (Phase 4), ResumeTailorPage (Phase 6)
│   │   │   ├── roadmap/          # RoadmapPage (Phase 5)
│   │   │   ├── DashboardPage.tsx # Overview dashboard
│   │   │   ├── ProfilePage.tsx   # User profile management
│   │   │   └── NotFoundPage.tsx  # 404 page
│   │   ├── App.tsx               # Route hierarchy
│   │   └── main.tsx              # React DOM entrypoint
│   ├── .env.example              # Frontend environment template
│   └── package.json
│
├── backend/                      # Java Spring Boot 3 REST API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/careerpilot/
│   │   │   │   ├── config/       # SecurityConfig, CorsConfig, JwtDecoderConfig
│   │   │   │   ├── controller/   # ApplicationController, CareerAnalyticsController, etc.
│   │   │   │   ├── dto/          # ApplicationDto, RemindersSummaryDto, CareerAnalyticsDto
│   │   │   │   ├── entity/       # Application, ApplicationHistory, ApplicationStatus
│   │   │   │   ├── exception/    # GlobalExceptionHandler, ResourceNotFoundException
│   │   │   │   ├── repository/   # ApplicationRepository, ApplicationHistoryRepository
│   │   │   │   ├── security/     # RestAuthenticationEntryPoint, SecurityUtils
│   │   │   │   ├── service/      # ApplicationService, CareerAnalyticsService, etc.
│   │   │   │   └── CareerPilotApplication.java
│   │   │   └── resources/
│   │   │       ├── db/
│   │   │       │   ├── migration/ # V1 through V7 Flyway migrations
│   │   │       │   └── seed/      # dev_seed.sql
│   │   │       └── application.yml
│   │   └── test/                 # 113 unit and integration tests (JUnit 5 + MockMvc)
│   ├── .env.example
│   └── pom.xml
│
├── docs/                         # Project Documentation
│   ├── architecture.md
│   ├── phase-1-authentication.md
│   ├── phase-2-ui-ux.md
│   ├── phase-3-job-discovery.md
│   ├── phase-4-resume-analyzer.md
│   ├── phase-5-career-roadmap.md
│   ├── phase-6-ai-resume-tailoring.md
│   └── phase-7-application-tracking-and-analytics.md
└── README.md
```

---

## 3. Database Migrations

Database tables and Row Level Security (RLS) policies are version-controlled with Flyway:
- `V1__init_careerpilot_schema.sql`: Initial schema (profiles, skills, user_skills, jobs, applications).
- `V2__enable_rls_policies.sql`: Enables RLS across base tables.
- `V3__phase3_jobs_and_saved_jobs.sql`: Job discovery and saved jobs schema.
- `V4__phase4_resumes_and_analyses.sql`: Resumes and resume analyses schema.
- `V5__phase5_career_roadmaps.sql`: Career roadmaps and milestones schema.
- `V6__phase6_resume_tailoring.sql`: Resume tailoring sessions, structured suggestions, and versioned resumes.
- `V7__phase7_application_tracking_and_history.sql`: Extended application tracking, manual requisitions, and audit history.

---

## 4. REST API Endpoints (Phase 7 Highlights)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/applications` | Authenticated | Paginated search, filter, and sorted applications |
| `GET` | `/api/applications/all` | Authenticated | Unpaginated application pipeline for Kanban view |
| `POST` | `/api/applications` | Authenticated | Create an application (catalog job or manual entry) |
| `GET` | `/api/applications/{id}` | Authenticated | Retrieve application details and full audit history |
| `PUT` | `/api/applications/{id}` | Authenticated | Update application details, contacts, and reminders |
| `PATCH` | `/api/applications/{id}/status` | Authenticated | Update application status and record audit log |
| `DELETE` | `/api/applications/{id}` | Authenticated | Delete application and its historical transitions |
| `GET` | `/api/applications/reminders` | Authenticated | Retrieve due follow-ups, upcoming interviews & stale items |
| `GET` | `/api/analytics/applications` | Authenticated | Server-side computed career analytics & timeline aggregates |

---

## 5. How to Run Locally

### Terminal 1: Backend
```bash
cd backend
mvn spring-boot:run
```

### Terminal 2: Frontend
```bash
cd frontend
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## 6. Verification & Tests

```bash
# 1. Run frontend build & type check (0 errors)
cd frontend
npm run build

# 2. Run backend test suite (113 passing unit & integration tests)
cd ../backend
mvn test
```

For complete technical specifications, see [docs/phase-7-application-tracking-and-analytics.md](file:///c:/osman/career_pilot/docs/phase-7-application-tracking-and-analytics.md).
