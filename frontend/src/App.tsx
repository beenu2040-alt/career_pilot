import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import {
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
import { JobsPage } from "@/pages/jobs/JobsPage";
import { ResumePage } from "@/pages/resume/ResumePage";
import { ResumeTailorPage } from "@/pages/resume/ResumeTailorPage";
import { ApplicationsPage } from "@/pages/applications/ApplicationsPage";
import { RoadmapPage } from "@/pages/roadmap/RoadmapPage";
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

              {/* 1. Find Jobs (Phase 3 Full Feature) */}
              <Route path="jobs" element={<JobsPage />} />

              {/* 2. Resume Analyzer (Phase 4 Full Feature) */}
              <Route path="resume-analyzer" element={<ResumePage />} />
              <Route path="resume" element={<Navigate to="/resume-analyzer" replace />} />

              {/* 3. Career Roadmap (Phase 5 Full Feature) */}
              <Route path="roadmap" element={<RoadmapPage />} />

              {/* 4. Resume Tailor (Phase 6 Full Feature) */}
              <Route path="resume-tailor" element={<ResumeTailorPage />} />

              {/* 5. Applications (Phase 7 Full Feature) */}
              <Route path="applications" element={<ApplicationsPage />} />

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
