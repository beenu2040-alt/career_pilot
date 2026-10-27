import React from "react";
import { Link } from "react-router-dom";
import {
  FileText,
  Compass,
  Briefcase,
  Send,
  ArrowRight,
  CheckCircle2,
  TrendingUp,
  ShieldCheck,
  ChevronRight,
  Sparkles,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { useAuth } from "@/context/AuthContext";

export const LandingPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="space-y-24 py-12 md:py-16">
      {/* 1. HERO SECTION */}
      <section className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 text-center space-y-8">
        <div className="max-w-3xl mx-auto space-y-4">
          <Badge variant="info" className="px-3 py-1 text-xs font-semibold gap-1.5 inline-flex">
            <Sparkles className="h-3.5 w-3.5 text-blue-600" />
            Phase 2 Architecture Ready
          </Badge>

          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-slate-900 leading-tight">
            Build a career that moves you forward.
          </h1>

          <p className="text-lg sm:text-xl text-slate-600 leading-relaxed max-w-2xl mx-auto">
            Understand your strengths, discover opportunities that fit your skills, and turn your career goals into an actionable plan.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-4 pt-4">
            <Link to={user ? "/dashboard" : "/register"}>
              <Button size="lg" className="h-12 px-7 text-sm font-semibold bg-blue-700 hover:bg-blue-800 text-white shadow-md gap-2">
                {user ? "Go to Dashboard" : "Get Started"} <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
            <a href="#features">
              <Button variant="outline" size="lg" className="h-12 px-7 text-sm font-semibold text-slate-700 border-slate-300 hover:bg-slate-50 gap-2">
                Explore Features <ChevronRight className="h-4 w-4 text-slate-400" />
              </Button>
            </a>
          </div>
        </div>

        {/* 2. PRODUCT PREVIEW MOCKUP (Built with actual frontend components) */}
        <div id="preview" className="pt-6 max-w-5xl mx-auto">
          <div className="rounded-2xl border border-slate-200/90 bg-white p-3 sm:p-5 shadow-xl shadow-slate-200/50">
            {/* Mock Window Top Bar */}
            <div className="flex items-center justify-between pb-4 mb-4 border-b border-slate-100 px-2">
              <div className="flex items-center gap-2">
                <span className="h-3 w-3 rounded-full bg-slate-300" />
                <span className="h-3 w-3 rounded-full bg-slate-300" />
                <span className="h-3 w-3 rounded-full bg-slate-300" />
                <span className="ml-3 text-xs font-mono text-slate-400">careerpilot.ai/dashboard</span>
              </div>
              <Badge variant="outline" className="text-[11px] text-slate-500 font-normal">
                Live Interactive Architecture
              </Badge>
            </div>

            {/* Mock Dashboard Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-left">
              {/* Card 1: Target Role Diagnostic */}
              <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/70 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-slate-500">Target Role</span>
                  <Badge variant="success" className="text-[10px]">Verified Target</Badge>
                </div>
                <h4 className="font-bold text-slate-800 text-base">Principal Platform Architect</h4>
                <p className="text-xs text-muted-foreground">Target seniority: Staff / Principal</p>
                <div className="pt-2">
                  <div className="flex justify-between text-[11px] font-medium text-slate-600 mb-1">
                    <span>Skill Alignment</span>
                    <span className="font-semibold text-blue-700">78% Match</span>
                  </div>
                  <div className="h-2 w-full bg-slate-200 rounded-full overflow-hidden">
                    <div className="h-full bg-blue-700 rounded-full w-[78%]" />
                  </div>
                </div>
              </div>

              {/* Card 2: Competency Breakdown */}
              <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/70 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-slate-500">Skill Inventory</span>
                  <span className="text-xs font-mono text-emerald-600 font-semibold">+3 In Progress</span>
                </div>
                <div className="space-y-1.5 pt-1">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-slate-700">Distributed Systems</span>
                    <span className="text-emerald-700 font-medium">Advanced</span>
                  </div>
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-slate-700">Cloud Architecture</span>
                    <span className="text-emerald-700 font-medium">Advanced</span>
                  </div>
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-slate-700">Event-Driven Streaming</span>
                    <span className="text-amber-700 font-medium">Gap Identified</span>
                  </div>
                </div>
              </div>

              {/* Card 3: Progression Pipeline */}
              <div className="p-4 rounded-xl border border-slate-100 bg-slate-50/70 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-slate-500">Application Pipeline</span>
                  <Badge variant="secondary" className="text-[10px]">Active</Badge>
                </div>
                <div className="grid grid-cols-3 gap-2 text-center pt-1">
                  <div className="p-2 bg-white rounded-lg border border-slate-200/60">
                    <div className="text-base font-bold text-slate-800">4</div>
                    <div className="text-[10px] text-muted-foreground">Targeted</div>
                  </div>
                  <div className="p-2 bg-white rounded-lg border border-slate-200/60">
                    <div className="text-base font-bold text-blue-700">2</div>
                    <div className="text-[10px] text-muted-foreground">Interviewing</div>
                  </div>
                  <div className="p-2 bg-white rounded-lg border border-slate-200/60">
                    <div className="text-base font-bold text-emerald-600">1</div>
                    <div className="text-[10px] text-muted-foreground">Offer Stage</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 3. CORE FEATURES SECTION */}
      <section id="features" className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 space-y-12">
        <div className="text-center max-w-2xl mx-auto space-y-3">
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
            Integrated Tools for Career Progression
          </h2>
          <p className="text-sm sm:text-base text-slate-600 leading-relaxed">
            Four focused engines working cohesively to transform career planning from guesswork into a clear, measurable roadmap.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {/* Feature 1 */}
          <Card className="hover:shadow-md transition-shadow border-slate-200">
            <CardHeader className="pb-3">
              <div className="h-10 w-10 rounded-lg bg-blue-50 flex items-center justify-center text-blue-700 mb-3">
                <FileText className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Resume Analysis</CardTitle>
              <CardDescription>Targeted ATS & Content Evaluation</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-slate-600 space-y-2">
              <p>
                Analyze structural clarity, bullet-point impact metrics, and keywords against target industry job descriptions.
              </p>
              <div className="pt-2 text-[11px] font-semibold text-blue-700 flex items-center gap-1">
                <span>Phase 3 Roadmap</span> &rarr;
              </div>
            </CardContent>
          </Card>

          {/* Feature 2 */}
          <Card className="hover:shadow-md transition-shadow border-slate-200">
            <CardHeader className="pb-3">
              <div className="h-10 w-10 rounded-lg bg-indigo-50 flex items-center justify-center text-indigo-700 mb-3">
                <Briefcase className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Job Matching</CardTitle>
              <CardDescription>Precision Opportunity Search</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-slate-600 space-y-2">
              <p>
                Discover roles aligned with your verified competencies and clear preferences for location, work style, and level.
              </p>
              <div className="pt-2 text-[11px] font-semibold text-indigo-700 flex items-center gap-1">
                <span>Phase 3 Roadmap</span> &rarr;
              </div>
            </CardContent>
          </Card>

          {/* Feature 3 */}
          <Card className="hover:shadow-md transition-shadow border-slate-200">
            <CardHeader className="pb-3">
              <div className="h-10 w-10 rounded-lg bg-emerald-50 flex items-center justify-center text-emerald-700 mb-3">
                <Compass className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Career Roadmaps</CardTitle>
              <CardDescription>Actionable Skill Gap Diagnostics</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-slate-600 space-y-2">
              <p>
                Identify critical missing competencies for target roles and generate concrete milestones to bridge the gap.
              </p>
              <div className="pt-2 text-[11px] font-semibold text-emerald-700 flex items-center gap-1">
                <span>Phase 3 Roadmap</span> &rarr;
              </div>
            </CardContent>
          </Card>

          {/* Feature 4 */}
          <Card className="hover:shadow-md transition-shadow border-slate-200">
            <CardHeader className="pb-3">
              <div className="h-10 w-10 rounded-lg bg-violet-50 flex items-center justify-center text-violet-700 mb-3">
                <Send className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Application Tracker</CardTitle>
              <CardDescription>Unified Search Management</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-slate-600 space-y-2">
              <p>
                Organize pipeline opportunities from initial discovery through interviews and final compensation negotiations.
              </p>
              <div className="pt-2 text-[11px] font-semibold text-violet-700 flex items-center gap-1">
                <span>Phase 3 Roadmap</span> &rarr;
              </div>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* 4. HOW IT WORKS (3 Simple Steps) */}
      <section id="how-it-works" className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 space-y-12">
        <div className="text-center max-w-2xl mx-auto space-y-3">
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
            How CareerPilot Works
          </h2>
          <p className="text-sm sm:text-base text-slate-600">
            A methodical, three-phase framework designed to guide every stage of your career strategy.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Step 1 */}
          <div className="p-6 rounded-2xl border border-slate-200 bg-white space-y-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-700 text-white font-bold text-sm">
              1
            </div>
            <h3 className="font-bold text-base text-slate-900">Map Your Skills & Profile</h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              Define your professional target, location preferences, and core technical competencies in your verified profile.
            </p>
            <div className="pt-2 flex items-center gap-1.5 text-xs text-emerald-700 font-medium">
              <CheckCircle2 className="h-3.5 w-3.5" /> Phase 1 Profile Ready
            </div>
          </div>

          {/* Step 2 */}
          <div className="p-6 rounded-2xl border border-slate-200 bg-white space-y-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-700 text-white font-bold text-sm">
              2
            </div>
            <h3 className="font-bold text-base text-slate-900">Uncover Gaps & Opportunities</h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              Compare your experience profile with current market requirements to isolate specific skills that will advance your career.
            </p>
            <div className="pt-2 flex items-center gap-1.5 text-xs text-blue-700 font-medium">
              <TrendingUp className="h-3.5 w-3.5" /> Systematic Benchmarking
            </div>
          </div>

          {/* Step 3 */}
          <div className="p-6 rounded-2xl border border-slate-200 bg-white space-y-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-700 text-white font-bold text-sm">
              3
            </div>
            <h3 className="font-bold text-base text-slate-900">Tailor, Apply & Track</h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              Tailor application materials for specific postings and monitor interview stages throughout your job search.
            </p>
            <div className="pt-2 flex items-center gap-1.5 text-xs text-indigo-700 font-medium">
              <ShieldCheck className="h-3.5 w-3.5" /> Structured Pipeline
            </div>
          </div>
        </div>
      </section>

      {/* 5. CTA BANNER */}
      <section className="container mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
        <div className="rounded-2xl border border-blue-200 bg-gradient-to-br from-blue-50 to-indigo-50/50 p-8 sm:p-12 text-center space-y-6">
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
            Ready to structure your next career move?
          </h2>
          <p className="text-sm sm:text-base text-slate-600 max-w-xl mx-auto">
            Set up your profile, configure your target role, and explore the CareerPilot AI foundation.
          </p>
          <div className="pt-2">
            <Link to={user ? "/dashboard" : "/register"}>
              <Button size="lg" className="h-11 px-8 bg-blue-700 hover:bg-blue-800 text-white font-semibold shadow-md gap-2">
                {user ? "View Your Dashboard" : "Create Free Account"} <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
};
