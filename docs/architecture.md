# CareerPilot AI — System Architecture & Design Specification

## 1. Overview & Vision

**CareerPilot AI** is a production-minded SaaS platform designed to guide professionals through modern career management, resume optimization, career roadmap planning, and job application tracking. 

Phase 0 establishes the foundational monorepo structure, build configurations, security baselines, and health verification pipelines across both frontend and backend domains.

---

## 2. High-Level System Architecture

The following diagram illustrates the decoupled client-server architecture, local development origin negotiation, and external Supabase cloud database integration.

```mermaid
graph TD
    subgraph ClientLayer ["Client Layer (Frontend)"]
        Browser["User Web Browser"]
        ReactApp["CareerPilot React SPA (Vite + TS + Tailwind)"]
        Browser -->|Interacts with UI| ReactApp
    end

    subgraph APILayer ["API Layer (Backend)"]
        Gateway["Spring Security Filter Chain (CORS & Auth)"]
        HealthEndpoint["Health Controller (/api/health)"]
        FutureControllers["Phase 1+ Business Controllers"]
        JPA["Spring Data JPA / Hibernate"]

        Gateway -->|Allowed Origin: localhost:5173| HealthEndpoint
        Gateway -->|Stateless JWT (Phase 1)| FutureControllers
        FutureControllers --> JPA
    end

    subgraph DataLayer ["Data & Identity Layer"]
        SupabaseDB["Supabase Managed PostgreSQL (Session/Transaction Pooler)"]
        SupabaseAuth["Supabase Auth (JWT Provider - Phase 1)"]
        JPA -->|HikariCP / TLS| SupabaseDB
    end

    ReactApp -->|HTTP REST / JSON| Gateway
    ReactApp -.->|Direct Auth Tokens (Phase 1)| SupabaseAuth
```

---

## 3. Technology Stack Breakdown

| Tier | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Frontend Runtime** | React + TypeScript | 18.3.1 / TS 5.6 | Client user interface & type-safe state |
| **Build & Tooling** | Vite | 5.4.10 | Modern, rapid HMR and production bundler |
| **Styling & UI** | Tailwind CSS + shadcn/ui | 3.4.14 | Accessible, consistent SaaS design system |
| **Routing** | React Router | 6.28.0 | Declarative client-side route navigation |
| **Backend Runtime** | Java OpenJDK | 21 (LTS) | High-performance, modern server runtime |
| **Backend Framework** | Spring Boot | 3.3.5 | Production web framework, DI, lifecycle |
| **Security** | Spring Security | 6.x | Stateless filter chain, CORS policy, JWT hooks |
| **Persistence** | Spring Data JPA / Hibernate | 6.x | Relational mapping, schema validation |
| **Database** | PostgreSQL (Supabase) | 15+ | Scalable cloud PostgreSQL with pooling |
| **Authentication** | Supabase Auth (Planned) | Phase 1 | Identity management, MFA, JWT issuance |

---

## 4. Frontend Architecture (`frontend/`)

### 4.1 Structure
```
frontend/
├── src/
│   ├── components/
│   │   ├── layout/       # Header, Footer, AppLayout shell
│   │   └── ui/           # Reusable shadcn/ui primitives (Button, Card, Badge)
│   ├── lib/              # Utility functions (cn helper, api client)
│   ├── pages/            # View components (HomePage, PlaceholderPage, NotFoundPage)
│   ├── App.tsx           # Route declarations
│   ├── main.tsx          # Root mounting
│   └── index.css         # Tailwind directives & CSS variables
├── .env.example          # Environment variable template
├── package.json          # Dependencies & build scripts
├── tailwind.config.js    # Design tokens & color system
├── tsconfig.json         # TypeScript compiler configuration
└── vite.config.ts        # Vite build & alias configuration
```

### 4.2 Design System & Styling
- Utilizes **Tailwind CSS** paired with standard **shadcn/ui** primitives using CSS variables for semantic theme tokens (`--primary`, `--secondary`, `--muted`, `--card`, `--border`).
- Class merging is managed via `clsx` and `tailwind-merge` encapsulated in `cn()`.
- Responsive, mobile-first design with accessibility landmarks.

### 4.3 Client Routing
- Handled via `react-router-dom` with nested route configuration.
- Default route `/` loads the `HomePage` showcasing project status, live backend health diagnostics, and architecture roadmap.
- Future milestone routes (`/resume`, `/roadmap`, `/applications`) are cleanly mounted via `PlaceholderPage` components without mock authentication.
- Unmatched routes safely fallback to `NotFoundPage`.

---

## 5. Backend Architecture (`backend/`)

### 5.1 Structure
```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/careerpilot/
│   │   │   ├── config/        # Security, CORS, and Property configurations
│   │   │   ├── controller/    # REST endpoints (HealthController)
│   │   │   ├── dto/           # Data Transfer Objects / Records (HealthResponse)
│   │   │   └── CareerPilotApplication.java # Spring Boot entrypoint
│   │   └── resources/
│   │       └── application.yml # Database, server, and CORS configuration
│   └── test/
│       ├── java/com/careerpilot/
│       │   ├── controller/HealthControllerTest.java
│       │   └── CareerPilotApplicationTests.java
│       └── resources/
│           └── application-test.yml # In-memory database test config
├── .env.example               # Backend environment variable template
├── .mvn/                      # Maven configuration
└── pom.xml                    # Maven POM configuration
```

### 5.2 Security & CORS Model
1. **Stateless Session Management**: Spring Security is configured with `SessionCreationPolicy.STATELESS`. The backend does not maintain in-memory HTTP sessions or set session cookies.
2. **Explicit Development CORS**: Cross-Origin Resource Sharing is configured via a dedicated `CorsConfigurationSource` bean. The frontend development origin (`http://localhost:5173` and `http://127.0.0.1:5173`) is explicitly allowed. Allowed methods, headers, and credentials are explicitly defined.
3. **Public Probing**: The `/api/health` endpoint and CORS pre-flight `OPTIONS` requests are openly permitted for operational monitoring.
4. **Zero-Trust Baseline**: All other routes are protected (`anyRequest().authenticated()`), ready for the Supabase Auth JWT validation filter in Phase 1.

### 5.3 Health Endpoint Specification
- **Method**: `GET`
- **Path**: `/api/health`
- **Response Type**: `application/json`
- **Response Structure**:
  ```json
  {
    "status": "UP",
    "service": "CareerPilot AI API",
    "timestamp": "2026-10-26T14:00:00.000Z",
    "version": "0.1.0"
  }
  ```

---

## 6. Database & Persistence Strategy (Supabase PostgreSQL)

### 6.1 Database Connection Configuration
Supabase provides PostgreSQL database instances accessible via standard JDBC connection strings. All connection credentials are read strictly from environment variables:
- `SUPABASE_DB_URL`: JDBC URL pointing to Supabase transaction pooler (port 6543) or session pooler (port 5432).
- `SUPABASE_DB_USERNAME`: Database role (e.g., `postgres`).
- `SUPABASE_DB_PASSWORD`: Database user password.

### 6.2 Connection Pooling (HikariCP)
- Spring Boot uses HikariCP as the high-performance connection pool.
- `spring.datasource.hikari.initialization-fail-timeout: -1` is configured to ensure the application boots even if the external Supabase network connection is temporarily absent during local offline development.
- Maximum and minimum pool sizes are configurable via `DB_POOL_MAX_SIZE` and `DB_POOL_MIN_IDLE`.

### 6.3 Test Isolation
- Unit and slice tests execute against an isolated in-memory H2 database (or web-slice mock contexts) defined in `src/test/resources/application-test.yml`.
- No live network connection to Supabase is required to run backend unit/slice tests.

---

## 7. Environment & Secrets Management

Strict separation of concerns is maintained across environments:

1. **Frontend**:
   - Only non-sensitive parameters prefixed with `VITE_` are exposed (`VITE_API_BASE_URL`, `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`).
   - The Supabase `anon` key is designed to be public and works in conjunction with PostgreSQL Row Level Security (RLS).
   - Under no circumstances is the database connection string, database password, or Supabase `service_role` key exposed to the frontend.

2. **Backend**:
   - Manages all privileged credentials (`SUPABASE_DB_URL`, `SUPABASE_DB_PASSWORD`).
   - Environment variables are provided via shell exports or a local `.env` file that is strictly ignored by Git.

3. **Version Control Exclusion**:
   - `.env`, `.env.local`, and sensitive secrets are explicitly excluded from `.gitignore`.
   - `.env.example` templates contain only non-secret placeholders.

---

## 8. Development Roadmap

```mermaid
flowchart LR
    P0["Phase 0: Project Foundation"] --> P1["Phase 1: Auth & Profiles"]
    P1 --> P2["Phase 2: UI/UX & Design System"]
    P2 --> P3["Phase 3: Job Discovery & Matching"]
    P3 --> P4["Phase 4: AI Resume Analyzer (Current)"]
    P4 --> P5["Phase 5: Career Roadmap & Applications"]

    style P0 fill:#2563eb,stroke:#1d4ed8,color:#ffffff
    style P1 fill:#2563eb,stroke:#1d4ed8,color:#ffffff
    style P2 fill:#2563eb,stroke:#1d4ed8,color:#ffffff
    style P3 fill:#2563eb,stroke:#1d4ed8,color:#ffffff
    style P4 fill:#2563eb,stroke:#1d4ed8,color:#ffffff
    style P5 fill:#f8fafc,stroke:#cbd5e1,color:#0f172a
```

- **Phase 0 (Completed)**: Monorepo layout, React + Vite setup, Tailwind & shadcn UI shell, Spring Boot 3 with JPA & Security, Health endpoint, CORS policy, Supabase environment wiring, Git discipline.
- **Phase 1 (Completed)**: Supabase Auth integration, Spring Security JWT filter, Profile & Skills schema (`V1`, `V2`), Flyway migrations, Profile API.
- **Phase 2 (Completed)**: Reusable SaaS design system, consistent dashboard shell, responsive mobile sidebar/drawer, authentication guards, user settings.
- **Phase 3 (Completed)**: Job catalog search & filters, explainable skill matching engine, normalized `job_skills` and `saved_jobs` (`V3`), recommendations, bookmarking, skills inventory on profile.
- **Phase 4 (Completed)**: Secure PDF resume upload & parsing (PDFBox), Supabase/local storage isolation, 4-dimension explainable scoring rubric (0–100), LLM and deterministic fallback analysis, target role matching and bullet rewrites (`V4`).
- **Phase 5 (Next)**: Career trajectory graphs, milestone learning paths, and application tracking pipeline.
