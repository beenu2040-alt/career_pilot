# CareerPilot AI — Phase 2: Professional UI/UX & Dashboard

## 1. Overview & Vision

Phase 2 transitions **CareerPilot AI** into a cohesive SaaS product with an authentic, professional UI/UX design system. The user interface prioritizes clarity, typography hierarchy, high signal-to-noise ratio, and accessibility without relying on generic AI dashboard templates or ungrounded statistics.

---

## 2. Design System & Tokens

### Palette
- **Primary**: Deep Indigo (`hsl(226, 70%, 36%)` / Tailwind `#1c3fa8`), conveying precision, trust, and executive competence.
- **Backgrounds**: Soft neutral slate (`hsl(210, 20%, 98%)`), avoiding harsh stark-white glare.
- **Card Surfaces**: Pure white cards with subtle borders (`hsl(214, 32%, 91%)`) and soft shadows (`shadow-sm`).
- **Text & Slate Hierarchy**: High-contrast slate-900 for headings, slate-700 for labels, muted slate-500 for secondary descriptions.
- **Semantic Feedback**:
  - Success: Emerald (`hsl(142, 71%, 45%)`)
  - Warning / Review: Amber (`hsl(38, 92%, 50%)`)
  - Destructive / Error: Rose (`hsl(0, 84%, 60%)`)
  - Info / Roadmap: Sky / Blue (`hsl(217, 91%, 60%)`)

### Reusable UI Primitives (`frontend/src/components/ui/`)
1. **`Button`**: Primary, secondary, outline, ghost, destructive, link variants with consistent sizing (`sm`, `md`, `lg`).
2. **`Input`**: Standardized form input with floating/stacked label, left icon support, right icon support, helper text, and accessible error state messaging.
3. **`Badge`**: Status indicators with semantic variants (`default`, `secondary`, `success`, `warning`, `info`, `outline`).
4. **`Skeleton`**: Subtle animated pulse loaders for loading states in cards and metrics.
5. **`EmptyState`**: Standardized container for zero-data states featuring an icon, title, descriptive subtext, and call-to-action button.
6. **`Dialog`**: Accessible modal dialog with backdrop blur, keyboard Escape listener, and body scroll locking.
7. **`DropdownMenu`**: Click-outside-dismissible popover menu for user profile settings, account navigation, and sign out.

---

## 3. Application Shell & Layouts

### Public Shell (`PublicLayout.tsx`)
- Sticky top navigation bar with brand wordmark, feature anchor links, and auth CTAs (`Sign In`, `Get Started`, or `Go to Dashboard` if authenticated).
- Marketing footer with technology badges, legal attribution, and quick links.
- Responsive mobile adaptation.

### Authenticated Application Shell (`DashboardLayout.tsx`)
- **Desktop Sidebar**:
  - Collapsible width (64-col expanded, 20-col collapsed) with toggle control.
  - Active route highlighting with high-contrast indicator.
  - Roadmap phase tags (`Phase 3`, `Preview`) on future capability items.
  - Mini profile widget with user avatar and email.
  - Real-time Spring Boot API connection status badge with manual refresh ping.
- **Mobile Navigation Drawer**:
  - Full-screen backdrop overlay with tap-to-dismiss.
  - Clean touch-friendly navigation list.
  - Zero horizontal overflow across viewports.
- **Header**:
  - Contextual breadcrumbs (`CareerPilot / [Active Section]`).
  - Live backend health status indicator (`GET /api/health`).
  - User avatar dropdown menu linking to Profile, Settings, and Sign Out.

---

## 4. Route Architecture & Navigation

| Route | Shell Layout | Route Guard | Purpose |
|---|---|---|---|
| `/` | `PublicLayout` | Public | SaaS Landing Page (Hero, Mockup, Features, How It Works) |
| `/login` | `PublicLayout` | `PublicOnlyRoute` | Sign In with email & password |
| `/register` | `PublicLayout` | `PublicOnlyRoute` | Sign Up with full name, email, password |
| `/forgot-password` | `PublicLayout` | `PublicOnlyRoute` | Password reset link request |
| `/reset-password` | `PublicLayout` | `PublicOnlyRoute` | Set new password via Supabase recovery flow |
| `/dashboard` | `DashboardLayout` | `ProtectedRoute` | Core workspace dashboard (Metrics, Next steps, Activity) |
| `/profile` | `DashboardLayout` | `ProtectedRoute` | User Profile (connected to `GET`/`PUT /api/profile`) |
| `/jobs` | `DashboardLayout` | `ProtectedRoute` | Milestone placeholder for Job Matching Engine |
| `/resume-analyzer` | `DashboardLayout` | `ProtectedRoute` | Milestone placeholder for ATS Resume Parser |
| `/roadmap` | `DashboardLayout` | `ProtectedRoute` | Milestone placeholder for Career Roadmap & Skill Gap Engine |
| `/resume-tailor` | `DashboardLayout` | `ProtectedRoute` | Milestone placeholder for Contextual Bullet Tailoring |
| `/applications` | `DashboardLayout` | `ProtectedRoute` | Milestone placeholder for Pipeline Kanban Tracker |
| `/settings` | `DashboardLayout` | `ProtectedRoute` | Preview settings panel |
| `/resume` | — | `ProtectedRoute` | Redirect alias &rarr; `/resume-analyzer` |
| `*` | `PublicLayout` | Public | Accessible 404 page |

---

## 5. Dashboard Data & Real Calculations

The `/dashboard` view relies on verified data from the Spring Boot API:
- **Profile Completion**: Evaluates the 4 core profile attributes (`fullName`, `headline`, `location`, `targetRole`) retrieved via `GET /api/profile`. Displays 0%–100% completion bar and status count without mocked statistics.
- **Next Steps Checklist**: Dynamically shows completed or pending status depending on real user profile state.
- **Recent Activity**: Logs profile synchronization events linked to `profile.updatedAt`. If uninitialized, displays the reusable `EmptyState` component.
- **Milestone Metric Cards**: Saved Jobs, Applications, and Interviews explicitly note Phase 3 roadmap launch rather than showing fabricated counters.

---

## 6. Verification & Quality Assurance

- **Frontend Compilation**: `tsc && vite build` &rarr; 0 TypeScript errors, production bundle generated cleanly.
- **Backend Tests**: `mvn test` &rarr; 18/18 tests pass with 0 failures or errors.
- **Authentication Resilience**: Protected routes require valid Supabase session tokens; JWTs are validated server-side on all Spring Boot API endpoints.
