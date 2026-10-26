# CareerPilot AI — Phase 0: Project Foundation

CareerPilot AI is a production-minded career development SaaS application designed to help professionals navigate career transitions, optimize resumes for ATS standards, build customized skill development roadmaps, and track job application pipelines.

This repository represents the completed **Phase 0: Project Foundation**, establishing the decoupled monorepo architecture, build setups, security baselines, and health verification endpoints.

---

## 1. Technology Stack

- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui components, React Router
- **Backend**: Java 21 (LTS), Spring Boot 3.3.5 (Spring Web, Spring Validation, Spring Security, Spring Data JPA, PostgreSQL driver)
- **Database**: Supabase PostgreSQL (Managed PostgreSQL with connection pooling)
- **Authentication**: Supabase Auth (Planned for Phase 1)
- **Version Control**: Git

---

## 2. Monorepo Structure

```
collage_project/
├── frontend/                     # React 18 + TypeScript + Vite SPA
│   ├── src/
│   │   ├── components/           # UI primitives (shadcn/ui) & Layouts
│   │   ├── lib/                  # Utilities (cn helper, api client)
│   │   ├── pages/                # Views (HomePage, PlaceholderPage, NotFound)
│   │   ├── App.tsx               # Route declarations
│   │   ├── index.css             # Tailwind design tokens
│   │   └── main.tsx              # React DOM entrypoint
│   ├── .env.example              # Frontend environment template
│   ├── package.json              # NPM dependencies & scripts
│   ├── tailwind.config.js        # Tailwind CSS theme configuration
│   ├── tsconfig.json             # TypeScript compiler settings
│   └── vite.config.ts            # Vite bundler & alias configuration
│
├── backend/                      # Java Spring Boot 3 API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/careerpilot/
│   │   │   │   ├── config/       # SecurityConfig, CorsConfig, CorsProperties
│   │   │   │   ├── controller/   # HealthController (/api/health)
│   │   │   │   ├── dto/          # HealthResponse record
│   │   │   │   └── CareerPilotApplication.java
│   │   │   └── resources/
│   │   │       └── application.yml # Database, HikariCP, and CORS setup
│   │   └── test/
│   │       ├── java/com/careerpilot/ # HealthControllerTest, ContextTest
│   │       └── resources/
│   │           └── application-test.yml # In-memory H2 test config
│   ├── .env.example              # Backend environment template
│   └── pom.xml                   # Maven POM configuration
│
├── docs/                         # Project Documentation
│   └── architecture.md           # System architecture & design specification
│
├── .gitignore                    # Comprehensive secrets & build artifact exclusions
└── README.md                     # Getting started guide
```

---

## 3. Prerequisites

Ensure the following tools are installed on your workstation:
- **Node.js**: v20.x or higher (`node -v`)
- **npm**: v10.x or higher (`npm -v`)
- **Java**: JDK 21 LTS (`java -version`)
- **Maven**: 3.9+ (`mvn -v`)
- **Git**: 2.x+ (`git --version`)

---

## 4. Environment Configuration

### 4.1 Frontend Configuration

Copy the example environment file in `frontend/`:
```bash
cp frontend/.env.example frontend/.env
```

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `VITE_API_BASE_URL` | Base URL of the Spring Boot API | `http://localhost:8080/api` |
| `VITE_SUPABASE_URL` | Public Supabase project URL | `https://your-project-id.supabase.co` |
| `VITE_SUPABASE_ANON_KEY` | Public Supabase anon key | *(Obtain from Supabase Dashboard)* |

> **Security Note**: Never place the Supabase `service_role` key or database passwords in the frontend `.env`.

### 4.2 Backend Configuration

Copy the example environment file in `backend/`:
```bash
cp backend/.env.example backend/.env
```

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `SERVER_PORT` | Port for the backend server | `8080` |
| `SPRING_PROFILES_ACTIVE`| Active Spring profiles | `dev` |
| `SUPABASE_DB_URL` | JDBC URL for Supabase PostgreSQL | `jdbc:postgresql://<host>:5432/postgres?sslmode=require` |
| `SUPABASE_DB_USERNAME` | Supabase database username | `postgres` |
| `SUPABASE_DB_PASSWORD` | Supabase database password | *(User provided)* |
| `DB_POOL_MAX_SIZE` | Maximum HikariCP pool connections | `10` |
| `DB_POOL_MIN_IDLE` | Minimum HikariCP idle connections | `2` |
| `CORS_ALLOWED_ORIGINS` | Comma-delimited list of allowed origins | `http://localhost:5173,http://127.0.0.1:5173` |

---

## 5. Local Setup & Running

The frontend and backend are completely decoupled and can run independently.

### 5.1 Running the Backend

From the repository root:
```bash
cd backend

# Build and execute all tests
mvn test

# Run the Spring Boot application
mvn spring-boot:run
```
The backend starts on `http://localhost:8080`.

Verify the health check endpoint:
```bash
curl -i http://localhost:8080/api/health
```
Expected response:
```json
{
  "status": "UP",
  "service": "CareerPilot AI API",
  "timestamp": "2026-10-26T...",
  "version": "0.1.0"
}
```

### 5.2 Running the Frontend

In a separate terminal, from the repository root:
```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start the Vite development server
npm run dev
```
Open your browser at `http://localhost:5173`. The application shell will load and automatically verify connectivity to the backend via `GET /api/health`.

---

## 6. Verification & Quality Gates

Run the verification suite across both subprojects:

```bash
# 1. Frontend production build and TypeScript check
cd frontend && npm run build

# 2. Backend unit/slice tests and compilation
cd ../backend && mvn clean test
```

---

## 7. Connecting to Supabase PostgreSQL (When Ready)

1. Create a Supabase project at [supabase.com](https://supabase.com).
2. Go to **Project Settings** > **Database**.
3. Under **Connection string**, select **JDBC** (or copy host, port, db name).
4. Update `backend/.env` with your host, port, username, and password:
   ```bash
   SUPABASE_DB_URL=jdbc:postgresql://<db.your-project-ref.supabase.co>:5432/postgres?sslmode=require
   SUPABASE_DB_USERNAME=postgres
   SUPABASE_DB_PASSWORD=your_actual_password
   ```
5. Restart the backend application.

---

## 8. Next Steps (Phase 1 Preview)

- Implement Supabase Auth integration (JWT authentication filter in Spring Security).
- Connect user sessions between the React frontend and Spring Boot backend.
- Define JPA entities for user profiles, resumes, and career roadmaps.
- Implement the Resume Studio ATS optimization service.
