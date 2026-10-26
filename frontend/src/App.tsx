import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "@/context/AuthContext";
import { AppLayout } from "@/components/layout/AppLayout";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { PublicOnlyRoute } from "@/components/auth/PublicOnlyRoute";
import { HomePage } from "@/pages/HomePage";
import { LoginPage } from "@/pages/auth/LoginPage";
import { RegisterPage } from "@/pages/auth/RegisterPage";
import { ForgotPasswordPage } from "@/pages/auth/ForgotPasswordPage";
import { ResetPasswordPage } from "@/pages/auth/ResetPasswordPage";
import { ProfilePage } from "@/pages/ProfilePage";
import { PlaceholderPage } from "@/pages/PlaceholderPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<AppLayout />}>
            {/* Public Routes */}
            <Route index element={<HomePage />} />

            {/* Public-Only Authentication Routes (redirect to profile if already signed in) */}
            <Route element={<PublicOnlyRoute />}>
              <Route path="login" element={<LoginPage />} />
              <Route path="register" element={<RegisterPage />} />
              <Route path="forgot-password" element={<ForgotPasswordPage />} />
              <Route path="reset-password" element={<ResetPasswordPage />} />
            </Route>

            {/* Protected Routes (require authenticated Supabase session) */}
            <Route element={<ProtectedRoute />}>
              <Route path="profile" element={<ProfilePage />} />
              <Route
                path="resume"
                element={
                  <PlaceholderPage
                    title="Resume Studio"
                    phase="Phase 1 Ready"
                    description="AI-driven resume tailoring, formatting, and ATS-optimized score evaluation. (Business features commence in Phase 2)."
                  />
                }
              />
              <Route
                path="roadmap"
                element={
                  <PlaceholderPage
                    title="Career Roadmap"
                    phase="Phase 2 Planned"
                    description="Interactive career trajectory graphs, skill gap diagnostics, and progression milestones."
                  />
                }
              />
              <Route
                path="applications"
                element={
                  <PlaceholderPage
                    title="Application Tracker"
                    phase="Phase 3 Planned"
                    description="Job pipeline tracking, interview scheduling, and offer comparison tools."
                  />
                }
              />
            </Route>

            {/* 404 Fallback */}
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
