import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import {
  Search,
  FileText,
  Compass,
  Wand2,
  Send,
  Settings,
} from "lucide-react";
import { AuthProvider } from "@/context/AuthContext";
import { PublicLayout } from "@/components/layout/PublicLayout";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { PublicOnlyRoute } from "@/components/auth/PublicOnlyRoute";

import { LandingPage } from "@/pages/LandingPage";
import { LoginPage } from "@/pages/auth/LoginPage";
import { RegisterPage } from "@/pages/auth/RegisterPage";
import { ForgotPasswordPage } from "@/pages/auth/ForgotPasswordPage";
import { ResetPasswordPage } from "@/pages/auth/ResetPasswordPage";

import { DashboardPage } from "@/pages/DashboardPage";
import { ProfilePage } from "@/pages/ProfilePage";
import { PlaceholderPage } from "@/pages/PlaceholderPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public & Authentication Shell */}
          <Route element={<PublicLayout />}>
            <Route index element={<LandingPage />} />

            {/* Public-Only Authentication Routes (redirect to /dashboard if logged in) */}
            <Route element={<PublicOnlyRoute />}>
              <Route path="login" element={<LoginPage />} />
              <Route path="register" element={<RegisterPage />} />
              <Route path="forgot-password" element={<ForgotPasswordPage />} />
              <Route path="reset-password" element={<ResetPasswordPage />} />
            </Route>

            {/* 404 Fallback within Public Shell */}
            <Route path="404" element={<NotFoundPage />} />
          </Route>

          {/* Authenticated Dashboard Shell (Protected Routes) */}
          <Route element={<ProtectedRoute />}>
            <Route element={<DashboardLayout />}>
              <Route path="dashboard" element={<DashboardPage />} />
              <Route path="profile" element={<ProfilePage />} />

              {/* 1. Find Jobs (Phase 3 Roadmap) */}
              <Route
                path="jobs"
                element={
                  <PlaceholderPage
                    title="Find Jobs"
                    phase="Phase 3 Roadmap"
                    description="Search and discover curated job postings tailored to your verified competencies and preferred location."
                    icon={<Search className="h-7 w-7 text-indigo-700" />}
                    plannedFeatures={[
                      "Semantic role matching based on PostgreSQL skill profile",
                      "Direct filtering by employment type, location, and seniority",
                      "Automated alignment score indicator per requisition",
                      "One-click save to personal application pipeline",
                    ]}
                  />
                }
              />

              {/* 2. Resume Analyzer (Phase 3 Roadmap) */}
              <Route
                path="resume-analyzer"
                element={
                  <PlaceholderPage
                    title="Resume Analyzer"
                    phase="Phase 3 Roadmap"
                    description="Automated resume parsing, applicant tracking system (ATS) keyword matching, and section-by-section scoring."
                    icon={<FileText className="h-7 w-7 text-emerald-700" />}
                    plannedFeatures={[
                      "PDF/DOCX file upload and text extraction pipeline",
                      "ATS keyword density and formatting audit",
                      "Action verb strength and quantifiable metric detection",
                      "Target role compatibility scoring engine",
                    ]}
                  />
                }
              />

              {/* 3. Career Roadmap (Phase 3 Roadmap) */}
              <Route
                path="roadmap"
                element={
                  <PlaceholderPage
                    title="Career Roadmap"
                    phase="Phase 3 Roadmap"
                    description="Interactive skill trees, competency benchmarks, and targeted learning milestones toward your aspirational career tier."
                    icon={<Compass className="h-7 w-7 text-violet-700" />}
                    plannedFeatures={[
                      "Skill gap comparison against market requisitions",
                      "Milestone-driven learning path recommendation",
                      "Quarterly progress tracking and goal setting",
                      "Seniority-tier benchmark metrics",
                    ]}
                  />
                }
              />

              {/* 4. Resume Tailor (Phase 3 Roadmap) */}
              <Route
                path="resume-tailor"
                element={
                  <PlaceholderPage
                    title="Resume Tailor"
                    phase="Phase 3 Roadmap"
                    description="Targeted resume bullet customization matching specific job descriptions to maximize screening success."
                    icon={<Wand2 className="h-7 w-7 text-amber-700" />}
                    plannedFeatures={[
                      "Job description requirement alignment engine",
                      "Contextual bullet point rewrites emphasizing relevant achievements",
                      "Skill keyword highlighting and integration guidance",
                      "Versioned resume export tailored for individual submissions",
                    ]}
                  />
                }
              />

              {/* 5. Applications (Phase 3 Roadmap) */}
              <Route
                path="applications"
                element={
                  <PlaceholderPage
                    title="Application Tracker"
                    phase="Phase 3 Roadmap"
                    description="End-to-end pipeline management for job applications, interview schedules, recruiter contacts, and offers."
                    icon={<Send className="h-7 w-7 text-blue-700" />}
                    plannedFeatures={[
                      "Multi-stage Kanban tracking (Bookmarked, Applied, Screening, Interviewing, Offer)",
                      "Interview timeline logs and reminder notes",
                      "Salary negotiation benchmarking notes",
                      "Application status analytics and response rate metrics",
                    ]}
                  />
                }
              />

              {/* 6. Settings (Phase 2 Preview) */}
              <Route
                path="settings"
                element={
                  <PlaceholderPage
                    title="Account & Workspace Settings"
                    phase="Phase 2 Preview"
                    description="Manage your account preferences, authentication credentials, and notifications."
                    icon={<Settings className="h-7 w-7 text-slate-700" />}
                    plannedFeatures={[
                      "Account security & password rotation",
                      "Notification preferences & digests",
                      "Data export and privacy controls",
                      "Supabase identity credential management",
                    ]}
                  />
                }
              />

              {/* Backwards compatibility alias */}
              <Route path="resume" element={<Navigate to="/resume-analyzer" replace />} />
            </Route>
          </Route>

          {/* Catch-all 404 Route */}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
