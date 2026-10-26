import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AppLayout } from "@/components/layout/AppLayout";
import { HomePage } from "@/pages/HomePage";
import { PlaceholderPage } from "@/pages/PlaceholderPage";
import { NotFoundPage } from "@/pages/NotFoundPage";

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<AppLayout />}>
          <Route index element={<HomePage />} />
          <Route
            path="resume"
            element={
              <PlaceholderPage
                title="Resume Studio"
                phase="Phase 1 Planned"
                description="AI-driven resume tailoring, formatting, and ATS-optimized score evaluation."
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
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default App;
