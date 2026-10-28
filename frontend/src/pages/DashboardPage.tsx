import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  User as UserIcon,
  CheckCircle2,
  ArrowRight,
  Bookmark,
  Send,
  Calendar,
  Search,
  FileText,
  Compass,
  Wand2,
  Clock,
  Layers,
} from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { useAuth } from "@/context/AuthContext";
import { fetchUserProfile, fetchSavedJobs, type ProfileResponse } from "@/lib/api";

export const DashboardPage: React.FC = () => {
  const { user, accessToken } = useAuth();
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [savedJobsCount, setSavedJobsCount] = useState<number | null>(null);

  useEffect(() => {
    let isMounted = true;

    const loadProfile = async () => {
      if (!accessToken) {
        setIsLoadingProfile(false);
        return;
      }

      try {
        const [profileData, savedJobsData] = await Promise.all([
          fetchUserProfile(accessToken).catch(() => null),
          fetchSavedJobs(accessToken, 0, 1).catch(() => null),
        ]);

        if (isMounted) {
          if (profileData) setProfile(profileData);
          if (savedJobsData) setSavedJobsCount(savedJobsData.totalElements);
        }
      } catch {
        // Handled gracefully: profile remains null if 404 or backend down
      } finally {
        if (isMounted) {
          setIsLoadingProfile(false);
        }
      }
    };

    loadProfile();
    return () => {
      isMounted = false;
    };
  }, [accessToken]);

  // Calculate real profile completion percentage based on backend profile fields
  const calculateProfileCompletion = (): { percentage: number; completedCount: number; totalCount: number } => {
    if (!profile) return { percentage: 0, completedCount: 0, totalCount: 4 };

    const fields = [
      Boolean(profile.fullName && profile.fullName.trim().length > 0),
      Boolean(profile.headline && profile.headline.trim().length > 0),
      Boolean(profile.location && profile.location.trim().length > 0),
      Boolean(profile.targetRole && profile.targetRole.trim().length > 0),
    ];

    const completedCount = fields.filter(Boolean).length;
    const percentage = Math.round((completedCount / fields.length) * 100);
    return { percentage, completedCount, totalCount: fields.length };
  };

  const { percentage: profileCompletionPct, completedCount, totalCount } = calculateProfileCompletion();

  const displayName = profile?.fullName || user?.user_metadata?.full_name || user?.email?.split("@")[0] || "Professional";

  return (
    <div className="space-y-8">
      {/* 1. WELCOME SECTION */}
      <div className="rounded-2xl border border-slate-200/90 bg-white p-6 sm:p-8 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
                Welcome back, {isLoadingProfile ? <Skeleton className="h-8 w-36 inline-block" /> : displayName}
              </h2>
              <Badge variant="info" className="text-xs">
                Active Session
              </Badge>
            </div>
            <p className="text-sm text-muted-foreground max-w-2xl leading-relaxed">
              {profile
                ? `Targeting ${profile.targetRole || "your next career milestone"}. Keep your skills and application pipeline updated to stay on track.`
                : "Your CareerPilot account is ready. Complete your profile details below to enable targeted skill gap diagnostics and tailored job matching."}
            </p>
          </div>

          <div className="shrink-0 flex items-center gap-3">
            <Link to="/profile">
              <Button className="gap-2 bg-blue-700 hover:bg-blue-800 text-white shadow-sm">
                <UserIcon className="h-4 w-4" />
                {profile ? "Edit Profile" : "Complete Profile"}
              </Button>
            </Link>
          </div>
        </div>
      </div>

      {/* 2. SUMMARY METRICS GRID */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Metric 1: Real Profile Completion */}
        <Card className="hover:shadow-sm transition-shadow border-slate-200">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Profile Health
              </span>
              <div className="h-8 w-8 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center">
                <UserIcon className="h-4 w-4" />
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-2">
            {isLoadingProfile ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <div className="text-2xl font-bold text-slate-900">{profileCompletionPct}%</div>
            )}
            <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full bg-blue-700 rounded-full transition-all duration-500"
                style={{ width: `${profileCompletionPct}%` }}
              />
            </div>
            <p className="text-[11px] text-muted-foreground">
              {completedCount} of {totalCount} core attributes completed
            </p>
          </CardContent>
        </Card>

        {/* Metric 2: Saved Jobs */}
        <Link to="/jobs?tab=saved" className="block">
          <Card className="hover:shadow-sm hover:border-indigo-300 transition-all border-slate-200 cursor-pointer">
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Saved Jobs
                </span>
                <div className="h-8 w-8 rounded-lg bg-indigo-50 text-indigo-700 flex items-center justify-center">
                  <Bookmark className="h-4 w-4" />
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-1">
              <div className="text-2xl font-bold text-slate-900">
                {savedJobsCount !== null ? savedJobsCount : 0}
              </div>
              <p className="text-[11px] text-slate-500">
                {savedJobsCount && savedJobsCount > 0 ? "Bookmarked opportunities" : "View saved vacancies"}
              </p>
            </CardContent>
          </Card>
        </Link>

        {/* Metric 3: Active Applications */}
        <Card className="hover:shadow-sm transition-shadow border-slate-200">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Active Applications
              </span>
              <div className="h-8 w-8 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center">
                <Send className="h-4 w-4" />
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-1">
            <div className="text-2xl font-bold text-slate-900">0</div>
            <p className="text-[11px] text-slate-500">
              Applications pipeline opens in Phase 3
            </p>
          </CardContent>
        </Card>

        {/* Metric 4: Upcoming Interviews */}
        <Card className="hover:shadow-sm transition-shadow border-slate-200">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Interviews
              </span>
              <div className="h-8 w-8 rounded-lg bg-violet-50 text-violet-700 flex items-center justify-center">
                <Calendar className="h-4 w-4" />
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-1">
            <div className="text-2xl font-bold text-slate-900">0</div>
            <p className="text-[11px] text-slate-500">
              Interview scheduling connects in Phase 3
            </p>
          </CardContent>
        </Card>
      </div>

      {/* 3. TWO-COLUMN WORKFLOW: Checklist & Quick Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left 2 Cols: Recommended Next Steps Checklist */}
        <div className="lg:col-span-2 space-y-6">
          <Card className="border-slate-200">
            <CardHeader className="pb-3 border-b border-slate-100">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-base font-bold text-slate-900">
                    Recommended Next Steps
                  </CardTitle>
                  <CardDescription>
                    Structured onboarding path derived from real account status.
                  </CardDescription>
                </div>
                <Badge variant="outline" className="text-xs">
                  {completedCount > 0 ? "In Progress" : "Action Required"}
                </Badge>
              </div>
            </CardHeader>

            <CardContent className="p-0 divide-y divide-slate-100">
              {/* Item 1: Complete Profile */}
              <div className="flex items-start gap-4 p-5 hover:bg-slate-50/60 transition-colors">
                <div className="mt-0.5">
                  {profileCompletionPct === 100 ? (
                    <div className="flex h-6 w-6 items-center justify-center rounded-full bg-emerald-100 text-emerald-600">
                      <CheckCircle2 className="h-4 w-4" />
                    </div>
                  ) : (
                    <div className="flex h-6 w-6 items-center justify-center rounded-full border-2 border-slate-300 text-transparent">
                      •
                    </div>
                  )}
                </div>
                <div className="flex-1 space-y-1">
                  <div className="flex items-center justify-between">
                    <h4 className="text-sm font-semibold text-slate-900">
                      Complete your career profile
                    </h4>
                    <Link to="/profile">
                      <Button variant="ghost" size="sm" className="h-7 text-xs text-blue-700 gap-1">
                        {profileCompletionPct === 100 ? "Review" : "Complete"} <ArrowRight className="h-3 w-3" />
                      </Button>
                    </Link>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Provide your professional headline, location, and target role to tailor career recommendations.
                  </p>
                  <div className="pt-1">
                    <span className="text-[11px] font-medium text-slate-500">
                      Status: {profileCompletionPct === 100 ? "Completed" : `${completedCount}/4 fields filled`}
                    </span>
                  </div>
                </div>
              </div>

              {/* Item 2: Add Skills */}
              <div className="flex items-start gap-4 p-5 hover:bg-slate-50/60 transition-colors">
                <div className="mt-0.5">
                  <div className="flex h-6 w-6 items-center justify-center rounded-full border-2 border-dashed border-slate-300 text-slate-400 text-xs">
                    2
                  </div>
                </div>
                <div className="flex-1 space-y-1">
                  <div className="flex items-center justify-between">
                    <h4 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                      Personalized career roadmap & skill gaps
                      <Badge variant="info" className="text-[10px]">Active</Badge>
                    </h4>
                    <Link to="/roadmap">
                      <Button variant="ghost" size="sm" className="h-7 text-xs text-indigo-700 gap-1">
                        View Plan <ArrowRight className="h-3 w-3" />
                      </Button>
                    </Link>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Benchmark competencies against market vacancies, track milestones, and bridge skill gaps.
                  </p>
                </div>
              </div>

              {/* Item 3: Upload Resume */}
              <div className="flex items-start gap-4 p-5 hover:bg-slate-50/60 transition-colors">
                <div className="mt-0.5">
                  <div className="flex h-6 w-6 items-center justify-center rounded-full border-2 border-dashed border-indigo-300 text-indigo-600 text-xs font-semibold">
                    3
                  </div>
                </div>
                <div className="flex-1 space-y-1">
                  <div className="flex items-center justify-between">
                    <h4 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                      Upload resume for ATS evaluation
                      <Badge variant="info" className="text-[10px]">Active</Badge>
                    </h4>
                    <Link to="/resume-analyzer">
                      <Button variant="ghost" size="sm" className="h-7 text-xs text-indigo-700 gap-1">
                        Analyze <ArrowRight className="h-3 w-3" />
                      </Button>
                    </Link>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Extract keywords, check formatting, and score against target job requisitions.
                  </p>
                </div>
              </div>

              {/* Item 4: Explore Opportunities */}
              <div className="flex items-start gap-4 p-5 hover:bg-slate-50/60 transition-colors">
                <div className="mt-0.5">
                  <div className="flex h-6 w-6 items-center justify-center rounded-full border-2 border-dashed border-indigo-300 text-indigo-600 text-xs font-semibold">
                    4
                  </div>
                </div>
                <div className="flex-1 space-y-1">
                  <div className="flex items-center justify-between">
                    <h4 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                      Explore matching job opportunities
                      <Badge variant="info" className="text-[10px]">Active</Badge>
                    </h4>
                    <Link to="/jobs">
                      <Button variant="ghost" size="sm" className="h-7 text-xs text-indigo-700 gap-1">
                        Find Jobs <ArrowRight className="h-3 w-3" />
                      </Button>
                    </Link>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Discover high-match openings and begin building your structured pipeline.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Activity Panel */}
          <Card className="border-slate-200">
            <CardHeader className="pb-3 border-b border-slate-100">
              <CardTitle className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Clock className="h-4 w-4 text-slate-400" /> Recent Activity
              </CardTitle>
              <CardDescription>
                Auditable log of profile modifications and platform milestones.
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-5">
              {profile ? (
                <div className="space-y-4">
                  <div className="flex items-start gap-3 text-xs">
                    <div className="h-2 w-2 mt-1.5 rounded-full bg-blue-700 shrink-0" />
                    <div className="space-y-0.5">
                      <p className="font-semibold text-slate-800">
                        Profile synchronized with Supabase PostgreSQL
                      </p>
                      <p className="text-muted-foreground">
                        Last saved: {new Date(profile.updatedAt).toLocaleString()}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3 text-xs">
                    <div className="h-2 w-2 mt-1.5 rounded-full bg-emerald-600 shrink-0" />
                    <div className="space-y-0.5">
                      <p className="font-semibold text-slate-800">
                        Account created and session authenticated
                      </p>
                      <p className="text-muted-foreground">
                        Initial registration recorded
                      </p>
                    </div>
                  </div>
                </div>
              ) : (
                <EmptyState
                  icon={<Layers className="h-5 w-5 text-slate-400" />}
                  title="No activity recorded yet"
                  description="Complete your career profile to initialize your activity timeline."
                  actionText="Set Up Profile"
                  onAction={() => {}}
                />
              )}
            </CardContent>
          </Card>
        </div>

        {/* Right 1 Col: Quick Actions & Platform Roadmap */}
        <div className="space-y-6">
          {/* Quick Actions */}
          <Card className="border-slate-200">
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-bold text-slate-900">
                Quick Shortcuts
              </CardTitle>
              <CardDescription>
                Direct access to primary workspace workflows.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-2">
              <Link to="/profile" className="block">
                <Button variant="outline" className="w-full justify-start gap-2.5 text-xs h-10 border-slate-200 hover:bg-slate-50">
                  <UserIcon className="h-4 w-4 text-blue-700" />
                  <span>Update Career Profile</span>
                </Button>
              </Link>
              <Link to="/jobs" className="block">
                <Button variant="outline" className="w-full justify-start gap-2.5 text-xs h-10 border-slate-200 hover:bg-slate-50">
                  <Search className="h-4 w-4 text-indigo-700" />
                  <span>Browse Job Listings</span>
                </Button>
              </Link>
              <Link to="/resume-analyzer" className="block">
                <Button variant="outline" className="w-full justify-start gap-2.5 text-xs h-10 border-slate-200 hover:bg-slate-50">
                  <FileText className="h-4 w-4 text-emerald-700" />
                  <span>Analyze Resume ATS Score</span>
                </Button>
              </Link>
              <Link to="/roadmap" className="block">
                <Button variant="outline" className="w-full justify-start gap-2.5 text-xs h-10 border-slate-200 hover:bg-slate-50">
                  <Compass className="h-4 w-4 text-violet-700" />
                  <span>Career Roadmap & Gaps</span>
                </Button>
              </Link>
              <Link to="/resume-tailor" className="block">
                <Button variant="outline" className="w-full justify-start gap-2.5 text-xs h-10 border-slate-200 hover:bg-slate-50">
                  <Wand2 className="h-4 w-4 text-amber-600" />
                  <span>AI Resume Tailoring</span>
                </Button>
              </Link>
              <Link to="/applications" className="block">
                <Button variant="outline" className="w-full justify-start gap-2.5 text-xs h-10 border-slate-200 hover:bg-slate-50">
                  <Send className="h-4 w-4 text-blue-600" />
                  <span>Application Tracker & Pipeline</span>
                </Button>
              </Link>
            </CardContent>
          </Card>

          {/* Phase 2 Architecture Status Badge Card */}
          <Card className="border-slate-200 bg-slate-50/50">
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <CardTitle className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Architecture Status
                </CardTitle>
                <Badge variant="success" className="text-[10px]">Phase 2</Badge>
              </div>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground space-y-2.5">
              <div className="flex items-center gap-2 text-slate-700">
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                <span>Spring Security OAuth2 JWT Verified</span>
              </div>
              <div className="flex items-center gap-2 text-slate-700">
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                <span>Supabase PostgreSQL Schema Linked</span>
              </div>
              <div className="flex items-center gap-2 text-slate-700">
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                <span>Zero-Trust User Identity Enforced</span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};
