# CareerPilot AI — Career Development Platform

CareerPilot AI is a production-minded career development SaaS application designed to help professionals navigate career transitions, optimize resumes for ATS standards, build customized skill development roadmaps, and track job application pipelines.

This repository contains the completed **Phase 0 (Project Foundation)** and **Phase 1 (Database & Authentication)** implementations.

---

## 1. Technology Stack

- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui components, React Router, `@supabase/supabase-js`
- **Backend**: Java 21 (LTS), Spring Boot 3.3.5 (Spring Web, Spring Validation, Spring Security OAuth2 Resource Server, Spring Data JPA, Flyway, PostgreSQL driver)
- **Database**: Supabase PostgreSQL with version-controlled Flyway migrations & Row Level Security (RLS)
- **Authentication**: Supabase Auth (Email & Password registration, login, session persistence, and password recovery)
- **Backend Security**: Spring Security stateless filter chain with Supabase JWT signature and claims validation
- **Version Control**: Git

---

## 2. Monorepo Structure

```
collage_project/
├── frontend/                     # React 18 + TypeScript + Vite SPA
│   ├── src/
│   │   ├── components/
│   │   │   ├── auth/             # ProtectedRoute, PublicOnlyRoute
│   │   │   ├── layout/           # Header, Footer, AppLayout
│   │   │   └── ui/               # Button, Card, Badge (shadcn/ui primitives)
│   │   ├── context/              # AuthContext (Supabase Auth state & actions)
│   │   ├── lib/                  # supabase client, api client, utils
│   │   ├── pages/
│   │   │   ├── auth/             # LoginPage, RegisterPage, ForgotPassword, ResetPassword
│   │   │   ├── HomePage.tsx      # Landing & diagnostic overview
│   │   │   ├── ProfilePage.tsx   # User profile management (GET/PUT /api/profile)
│   │   │   ├── PlaceholderPage.tsx # Phase 2 & 3 milestones
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
│   │   │   │   ├── controller/   # HealthController, UserController, ProfileController
│   │   │   │   ├── dto/          # HealthResponse, UserMeResponse, ProfileResponse/Request
│   │   │   │   ├── entity/       # Profile, Skill, UserSkill, Job, Application
│   │   │   │   ├── exception/    # GlobalExceptionHandler, ResourceNotFoundException
│   │   │   │   ├── repository/   # ProfileRepository, SkillRepository, etc.
│   │   │   │   ├── security/     # RestAuthenticationEntryPoint, SecurityUtils
│   │   │   │   ├── service/      # ProfileService
│   │   │   │   └── CareerPilotApplication.java
│   │   │   └── resources/
│   │   │       ├── db/
│   │   │       │   ├── migration/ # V1__init_careerpilot_schema.sql, V2__enable_rls_policies.sql
│   │   │       │   └── seed/      # dev_seed.sql (sample skills and jobs)
│   │   │       └── application.yml # Datasource, Hikari, JWT, and CORS setup
│   │   └── test/
│   │       ├── java/com/careerpilot/ # HealthControllerTest, UserControllerTest, ProfileControllerTest, etc.
│   │       └── resources/
│   │           └── application-test.yml # Isolated in-memory H2 configuration
│   ├── .env.example              # Backend environment template
│   └── pom.xml
│
├── docs/                         # Project Documentation
│   ├── architecture.md           # High-level architecture & design
│   └── phase-1-authentication.md # Comprehensive Supabase Auth & JWT setup guide
│
├── .gitignore                    # Secrets, node_modules, target/ exclusions
└── README.md
```

---

## 3. Environment Configuration

### 3.1 Frontend (`frontend/.env`)
Copy `frontend/.env.example` to `frontend/.env`:
```bash
cp frontend/.env.example frontend/.env
```
Fill in your Supabase project keys (Project Settings &gt; API):
```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_SUPABASE_URL=https://<your-project-ref>.supabase.co
VITE_SUPABASE_ANON_KEY=<your-supabase-public-anon-key>
```

### 3.2 Backend (`backend/.env`)
Copy `backend/.env.example` to `backend/.env`:
```bash
cp backend/.env.example backend/.env
```
Configure your Supabase connection and JWT secret:
```bash
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Supabase PostgreSQL JDBC connection string
SUPABASE_DB_URL=jdbc:postgresql://<db.your-project-ref.supabase.co>:5432/postgres?sslmode=require
SUPABASE_DB_USERNAME=postgres
SUPABASE_DB_PASSWORD=<your-database-password>

# Enable Flyway migrations
FLYWAY_ENABLED=true

# Supabase JWT Secret (Project Settings > API > JWT Settings > JWT Secret)
SUPABASE_JWT_SECRET=<your-supabase-jwt-secret-minimum-32-chars>
```

---

## 4. Database Migrations

Database tables and Row Level Security (RLS) policies are version-controlled with Flyway:
- `V1__init_careerpilot_schema.sql`: Creates `profiles`, `skills`, `user_skills`, `jobs`, and `applications` tables with constraints, indexes, and cascades.
- `V2__enable_rls_policies.sql`: Enables RLS across all tables for direct Supabase client access.
- `dev_seed.sql`: Isolated seed script containing tech skills and sample job postings for development.

When connected to Supabase PostgreSQL, migrations execute automatically upon starting the backend with `FLYWAY_ENABLED=true`.

---

## 5. API Endpoints

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Public | Service health & version diagnostic |
| `OPTIONS` | `/**` | Public | CORS preflight negotiation |
| `GET` | `/api/me` | Authenticated | Current user identity, email, and roles from JWT |
| `GET` | `/api/profile` | Authenticated | Retrieve authenticated user's profile |
| `PUT` | `/api/profile` | Authenticated | Create or update authenticated user's profile |

---

## 6. How to Run Locally

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

## 7. Verification & Tests

```bash
# 1. Run frontend build & type check
cd frontend && npm run build

# 2. Run backend test suite (18 unit & integration tests)
cd ../backend && mvn test
```

For full setup details and architecture decisions, see [docs/phase-1-authentication.md](file:///home/captain/collage_project/docs/phase-1-authentication.md).
