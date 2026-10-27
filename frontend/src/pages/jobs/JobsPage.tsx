import React, { useState, useEffect, useCallback } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  Search,
  MapPin,
  Briefcase,
  Building,
  Bookmark,
  Sparkles,
  Filter,
  RefreshCw,
  AlertCircle,
  ArrowRight,
  X,
  UserCheck,
  CheckCircle2,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { JobDetailModal } from "@/components/jobs/JobDetailModal";
import {
  type JobSummaryResponse,
  type PageResponse,
  type JobFilterParams,
  fetchJobs,
  fetchSavedJobs,
  fetchJobRecommendations,
  saveJob,
  unsaveJob,
  fetchUserSkills,
} from "@/lib/api";
import { useAuth } from "@/context/AuthContext";

type ViewTab = "all" | "recommended" | "saved";

export const JobsPage: React.FC = () => {
  const { user, accessToken } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Tab state derived from URL param or default "all"
  const activeTab = (searchParams.get("tab") as ViewTab) || "all";

  // Filter state
  const [searchTerm, setSearchTerm] = useState(searchParams.get("search") || "");
  const [locationTerm, setLocationTerm] = useState(searchParams.get("location") || "");
  const [employmentType, setEmploymentType] = useState(searchParams.get("employmentType") || "");
  const [remoteType, setRemoteType] = useState(searchParams.get("remoteType") || "");
  const [experienceLevel, setExperienceLevel] = useState(searchParams.get("experienceLevel") || "");
  const [skillFilter, setSkillFilter] = useState(searchParams.get("skill") || "");

  // Data & loading state
  const [jobsPage, setJobsPage] = useState<PageResponse<JobSummaryResponse> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [userSkillsCount, setUserSkillsCount] = useState<number | null>(null);

  // Selected job for detail modal
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);

  // Check user skills count when recommended tab is opened
  useEffect(() => {
    if (user && accessToken && activeTab === "recommended") {
      fetchUserSkills(accessToken)
        .then((skills) => setUserSkillsCount(skills.length))
        .catch(() => setUserSkillsCount(0));
    }
  }, [user, accessToken, activeTab]);

  // Load jobs based on active tab & filters
  const loadJobs = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      if (activeTab === "saved") {
        if (!accessToken) {
          setJobsPage(null);
          setIsLoading(false);
          return;
        }
        const data = await fetchSavedJobs(accessToken, currentPage, 10);
        setJobsPage(data);
      } else if (activeTab === "recommended") {
        if (!accessToken) {
          setJobsPage(null);
          setIsLoading(false);
          return;
        }
        const data = await fetchJobRecommendations(accessToken, currentPage, 10);
        setJobsPage(data);
      } else {
        const filterParams: JobFilterParams = {
          search: searchTerm || undefined,
          location: locationTerm || undefined,
          employmentType: employmentType || undefined,
          remoteType: remoteType || undefined,
          experienceLevel: experienceLevel || undefined,
          skill: skillFilter || undefined,
          page: currentPage,
          size: 10,
        };
        const data = await fetchJobs(filterParams, accessToken);
        setJobsPage(data);
      }
    } catch (err) {
      setError((err as Error).message || "Failed to load job listings.");
    } finally {
      setIsLoading(false);
    }
  }, [activeTab, accessToken, currentPage, searchTerm, locationTerm, employmentType, remoteType, experienceLevel, skillFilter]);

  useEffect(() => {
    loadJobs();
  }, [loadJobs]);

  const handleTabChange = (tab: ViewTab) => {
    setCurrentPage(0);
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set("tab", tab);
      return next;
    });
  };

  const handleApplyFilters = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    loadJobs();
  };

  const handleResetFilters = () => {
    setSearchTerm("");
    setLocationTerm("");
    setEmploymentType("");
    setRemoteType("");
    setExperienceLevel("");
    setSkillFilter("");
    setCurrentPage(0);
  };

  const handleToggleBookmark = async (jobId: string, currentSaved: boolean, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!accessToken) return;

    try {
      if (currentSaved) {
        await unsaveJob(jobId, accessToken);
      } else {
        await saveJob(jobId, accessToken);
      }

      // Optimistic update locally
      setJobsPage((prev) => {
        if (!prev) return null;
        const updated = prev.content.map((j) => (j.id === jobId ? { ...j, isSaved: !currentSaved } : j));
        return { ...prev, content: updated };
      });
    } catch (err) {
      console.error("Bookmark toggle failed:", err);
    }
  };

  const formatSalary = (min: number | null, max: number | null, currency: string) => {
    if (!min && !max) return null;
    const formatNum = (n: number) => `$${Math.round(n / 1000)}k`;
    if (min && max) return `${formatNum(min)} - ${formatNum(max)} ${currency}`;
    if (min) return `From ${formatNum(min)} ${currency}`;
    return `Up to ${formatNum(max!)} ${currency}`;
  };

  const hasActiveFilters = Boolean(
    searchTerm || locationTerm || employmentType || remoteType || experienceLevel || skillFilter
  );

  return (
    <div className="space-y-6">
      {/* Top Header Banner */}
      <div className="rounded-2xl border border-slate-200/90 bg-white p-6 sm:p-8 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
                Job Discovery & Matching
              </h2>
              <Badge variant="success" className="text-xs">
                Phase 3 Engine Active
              </Badge>
            </div>
            <p className="text-xs sm:text-sm text-muted-foreground max-w-2xl leading-relaxed">
              Discover opportunities with deterministic, explainable skill alignment. Inspect matching competencies, identify actionable gaps, and track saved positions.
            </p>
          </div>

          <div className="shrink-0 flex items-center gap-3">
            <Link to="/profile">
              <Button variant="outline" size="sm" className="gap-1.5 text-xs h-9 border-slate-200">
                <UserCheck className="h-4 w-4 text-blue-700" /> Manage My Skills
              </Button>
            </Link>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-2 border-b border-slate-100 pt-6 mt-2">
          <button
            onClick={() => handleTabChange("all")}
            className={`pb-3 px-1 text-xs font-semibold tracking-wide border-b-2 transition-colors ${
              activeTab === "all"
                ? "border-blue-700 text-blue-700"
                : "border-transparent text-slate-500 hover:text-slate-900"
            }`}
          >
            Explore All Openings
          </button>

          <button
            onClick={() => handleTabChange("recommended")}
            className={`pb-3 px-1 text-xs font-semibold tracking-wide border-b-2 transition-colors flex items-center gap-1.5 ${
              activeTab === "recommended"
                ? "border-blue-700 text-blue-700"
                : "border-transparent text-slate-500 hover:text-slate-900"
            }`}
          >
            <Sparkles className="h-3.5 w-3.5 text-blue-600" />
            Recommended For You
          </button>

          <button
            onClick={() => handleTabChange("saved")}
            className={`pb-3 px-1 text-xs font-semibold tracking-wide border-b-2 transition-colors flex items-center gap-1.5 ${
              activeTab === "saved"
                ? "border-blue-700 text-blue-700"
                : "border-transparent text-slate-500 hover:text-slate-900"
            }`}
          >
            <Bookmark className="h-3.5 w-3.5 text-indigo-600" />
            Saved Jobs
          </button>
        </div>
      </div>

      {/* Search & Filter Bar (Only shown on Explore All tab) */}
      {activeTab === "all" && (
        <div className="rounded-2xl border border-slate-200/90 bg-white p-5 shadow-sm space-y-4">
          <form onSubmit={handleApplyFilters} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {/* Search keyword */}
              <Input
                label="Search Keywords"
                placeholder="Title, company, or keywords..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                leftIcon={<Search className="h-4 w-4" />}
              />

              {/* Location */}
              <Input
                label="Location"
                placeholder="City, state, or country..."
                value={locationTerm}
                onChange={(e) => setLocationTerm(e.target.value)}
                leftIcon={<MapPin className="h-4 w-4" />}
              />

              {/* Skill filter */}
              <Input
                label="Required Skill"
                placeholder="e.g. Java, React, Docker..."
                value={skillFilter}
                onChange={(e) => setSkillFilter(e.target.value)}
                leftIcon={<Briefcase className="h-4 w-4" />}
              />
            </div>

            {/* Dropdown Filters Row */}
            <div className="flex flex-wrap items-center gap-3 pt-1">
              {/* Remote Type */}
              <select
                value={remoteType}
                onChange={(e) => setRemoteType(e.target.value)}
                className="h-9 rounded-md border border-input bg-background px-3 py-1 text-xs text-foreground shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-600"
              >
                <option value="">All Remote Types</option>
                <option value="REMOTE">Remote</option>
                <option value="HYBRID">Hybrid</option>
                <option value="ONSITE">On-Site</option>
              </select>

              {/* Employment Type */}
              <select
                value={employmentType}
                onChange={(e) => setEmploymentType(e.target.value)}
                className="h-9 rounded-md border border-input bg-background px-3 py-1 text-xs text-foreground shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-600"
              >
                <option value="">All Employment Types</option>
                <option value="FULL_TIME">Full-Time</option>
                <option value="PART_TIME">Part-Time</option>
                <option value="CONTRACT">Contract</option>
                <option value="INTERNSHIP">Internship</option>
              </select>

              {/* Experience Level */}
              <select
                value={experienceLevel}
                onChange={(e) => setExperienceLevel(e.target.value)}
                className="h-9 rounded-md border border-input bg-background px-3 py-1 text-xs text-foreground shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-600"
              >
                <option value="">All Experience Levels</option>
                <option value="ENTRY_LEVEL">Entry Level</option>
                <option value="MID_LEVEL">Mid Level</option>
                <option value="SENIOR">Senior</option>
                <option value="LEAD">Lead / Staff</option>
                <option value="EXECUTIVE">Executive</option>
              </select>

              <div className="ml-auto flex items-center gap-2">
                {hasActiveFilters && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={handleResetFilters}
                    className="text-xs h-9 text-slate-500 hover:text-slate-800"
                  >
                    <X className="h-3.5 w-3.5 mr-1" /> Reset
                  </Button>
                )}
                <Button type="submit" size="sm" className="text-xs h-9 bg-blue-700 hover:bg-blue-800 text-white gap-1.5 shadow-sm">
                  <Filter className="h-3.5 w-3.5" /> Apply Filters
                </Button>
              </div>
            </div>
          </form>
        </div>
      )}

      {/* Recommended Tab Prompts */}
      {activeTab === "recommended" && !user && (
        <div className="rounded-2xl border border-blue-200 bg-blue-50/50 p-8 text-center space-y-3">
          <Sparkles className="h-8 w-8 text-blue-700 mx-auto" />
          <h3 className="text-base font-bold text-slate-900">Sign In to View Recommended Matches</h3>
          <p className="text-xs text-muted-foreground max-w-md mx-auto">
            Personalized job recommendations require an authenticated session to evaluate your skills catalog.
          </p>
          <div className="pt-2">
            <Link to="/login">
              <Button size="sm" className="bg-blue-700 hover:bg-blue-800 text-white text-xs h-9">
                Sign In to Your Account
              </Button>
            </Link>
          </div>
        </div>
      )}

      {activeTab === "recommended" && user && userSkillsCount === 0 && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50/60 p-6 sm:p-8 space-y-3 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-800 mx-auto">
            <AlertCircle className="h-6 w-6" />
          </div>
          <h3 className="text-base font-bold text-slate-900">No Skills Found in Your Profile</h3>
          <p className="text-xs text-slate-600 max-w-md mx-auto leading-relaxed">
            The explainable matching engine requires at least one cataloged skill to compute deterministic alignment scores.
          </p>
          <div className="pt-2">
            <Link to="/profile">
              <Button className="bg-blue-700 hover:bg-blue-800 text-white text-xs h-9 gap-1.5">
                Add Skills to Your Profile <ArrowRight className="h-3.5 w-3.5" />
              </Button>
            </Link>
          </div>
        </div>
      )}

      {/* Saved Tab Login Prompt */}
      {activeTab === "saved" && !user && (
        <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center space-y-3">
          <Bookmark className="h-8 w-8 text-slate-400 mx-auto" />
          <h3 className="text-base font-bold text-slate-900">Sign in to view saved jobs</h3>
          <p className="text-xs text-muted-foreground max-w-md mx-auto">
            Your bookmarked opportunities are safely synchronized with Supabase PostgreSQL.
          </p>
          <div className="pt-2">
            <Link to="/login">
              <Button size="sm" className="bg-blue-700 hover:bg-blue-800 text-white text-xs h-9">
                Sign In
              </Button>
            </Link>
          </div>
        </div>
      )}

      {/* Error Banner */}
      {error && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-xs text-rose-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
          <Button variant="outline" size="sm" onClick={loadJobs} className="h-8 text-xs border-rose-200 gap-1">
            <RefreshCw className="h-3 w-3" /> Retry
          </Button>
        </div>
      )}

      {/* Loading Skeletons */}
      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="rounded-xl border border-slate-200 bg-white p-5 space-y-3">
              <div className="flex justify-between">
                <Skeleton className="h-4 w-28" />
                <Skeleton className="h-4 w-16" />
              </div>
              <Skeleton className="h-6 w-3/4" />
              <div className="flex gap-2">
                <Skeleton className="h-5 w-20" />
                <Skeleton className="h-5 w-24" />
              </div>
              <Skeleton className="h-8 w-full" />
            </div>
          ))}
        </div>
      )}

      {/* Job Cards Grid */}
      {!isLoading && jobsPage && jobsPage.content.length > 0 && (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-xs text-muted-foreground px-1">
            <span>
              Showing {jobsPage.content.length} of {jobsPage.totalElements} vacancies
            </span>
            {activeTab === "recommended" && (
              <span className="text-blue-700 font-medium">Ranked by deterministic skill match %</span>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {jobsPage.content.map((job) => {
              const salaryFormatted = formatSalary(job.salaryMin, job.salaryMax, job.salaryCurrency);

              return (
                <div
                  key={job.id}
                  onClick={() => setSelectedJobId(job.id)}
                  className="group relative rounded-xl border border-slate-200 bg-white p-5 shadow-sm hover:shadow-md hover:border-blue-300 transition-all cursor-pointer flex flex-col justify-between"
                >
                  <div className="space-y-2.5">
                    {/* Top Row: Company, Sample tag & Bookmark button */}
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex items-center gap-1 truncate max-w-[200px]">
                          <Building className="h-3 w-3 text-slate-400" />
                          {job.company}
                        </span>
                        {job.isSample && (
                          <Badge variant="outline" className="text-[10px] bg-amber-50 text-amber-800 border-amber-200 py-0">
                            Sample
                          </Badge>
                        )}
                      </div>

                      {user && (
                        <button
                          onClick={(e) => handleToggleBookmark(job.id, job.isSaved, e)}
                          className="rounded-lg p-1.5 text-slate-400 hover:text-blue-700 hover:bg-slate-100 transition-colors"
                          title={job.isSaved ? "Remove bookmark" : "Save job"}
                        >
                          <Bookmark className={`h-4 w-4 ${job.isSaved ? "fill-blue-700 text-blue-700" : ""}`} />
                        </button>
                      )}
                    </div>

                    {/* Job Title */}
                    <h3 className="text-base font-bold text-slate-900 group-hover:text-blue-700 transition-colors">
                      {job.title}
                    </h3>

                    {/* Meta Badges */}
                    <div className="flex flex-wrap items-center gap-2 text-xs text-slate-600">
                      <span className="flex items-center gap-1 text-[11px]">
                        <MapPin className="h-3 w-3 text-slate-400" />
                        {job.location}
                      </span>
                      {job.remoteType && (
                        <Badge variant="secondary" className="text-[10px] py-0">
                          {job.remoteType}
                        </Badge>
                      )}
                      {job.employmentType && (
                        <Badge variant="outline" className="text-[10px] py-0 text-slate-600">
                          {job.employmentType.replace("_", " ")}
                        </Badge>
                      )}
                      {job.experienceLevel && (
                        <Badge variant="info" className="text-[10px] py-0">
                          {job.experienceLevel.replace("_", " ")}
                        </Badge>
                      )}
                    </div>

                    {/* Salary Highlight */}
                    {salaryFormatted && (
                      <p className="text-xs font-semibold text-emerald-700">
                        {salaryFormatted}
                      </p>
                    )}

                    {/* Required Skills Chips */}
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {job.requiredSkills.slice(0, 4).map((s) => (
                        <span
                          key={s.id}
                          className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 text-[11px] font-medium"
                        >
                          {s.name}
                        </span>
                      ))}
                      {job.requiredSkills.length > 4 && (
                        <span className="px-1.5 py-0.5 rounded-md text-slate-400 text-[10px]">
                          +{job.requiredSkills.length - 4} more
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Card Bottom Row: Match percentage badge & View details */}
                  <div className="border-t border-slate-100 pt-3 mt-4 flex items-center justify-between text-xs">
                    {job.matchPercentage !== null && job.matchPercentage !== undefined ? (
                      <div className="flex items-center gap-1.5">
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                            job.matchPercentage >= 70
                              ? "bg-emerald-100 text-emerald-800"
                              : job.matchPercentage >= 40
                              ? "bg-blue-100 text-blue-800"
                              : "bg-slate-100 text-slate-700"
                          }`}
                        >
                          <CheckCircle2 className="h-3 w-3" />
                          {job.matchPercentage}% Skill Match
                        </span>
                      </div>
                    ) : (
                      <span className="text-[11px] text-slate-400">
                        {job.requiredSkills.length === 0 ? "No required skills" : "Click to view"}
                      </span>
                    )}

                    <span className="font-semibold text-blue-700 group-hover:underline inline-flex items-center gap-0.5">
                      View Requisition &rarr;
                    </span>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Pagination Controls */}
          {jobsPage.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-slate-200 pt-4 px-1 text-xs">
              <Button
                variant="outline"
                size="sm"
                disabled={jobsPage.isFirst}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                className="h-8 text-xs"
              >
                Previous Page
              </Button>

              <span className="text-muted-foreground">
                Page {jobsPage.pageNumber + 1} of {jobsPage.totalPages}
              </span>

              <Button
                variant="outline"
                size="sm"
                disabled={jobsPage.isLast}
                onClick={() => setCurrentPage((p) => Math.min(jobsPage.totalPages - 1, p + 1))}
                className="h-8 text-xs"
              >
                Next Page
              </Button>
            </div>
          )}
        </div>
      )}

      {/* Empty State */}
      {!isLoading && jobsPage && jobsPage.content.length === 0 && (
        <EmptyState
          icon={<Briefcase className="h-6 w-6 text-slate-400" />}
          title={
            activeTab === "saved"
              ? "No saved jobs yet"
              : activeTab === "recommended"
              ? "No job recommendations match your profile"
              : "No vacancies match your search criteria"
          }
          description={
            activeTab === "saved"
              ? "Bookmark jobs from the Explore tab to compare and track them here."
              : activeTab === "recommended"
              ? "Try adding more technical competencies to your profile to expand matching opportunities."
              : "Try adjusting or clearing your filters to discover open roles."
          }
          actionText={hasActiveFilters ? "Clear All Filters" : "Browse All Jobs"}
          onAction={() => {
            if (hasActiveFilters) {
              handleResetFilters();
            } else {
              handleTabChange("all");
            }
          }}
        />
      )}

      {/* Job Detail Modal */}
      <JobDetailModal
        jobId={selectedJobId}
        isOpen={Boolean(selectedJobId)}
        onClose={() => setSelectedJobId(null)}
        onBookmarkToggled={(id, isSaved) => {
          setJobsPage((prev) => {
            if (!prev) return null;
            const updated = prev.content.map((j) => (j.id === id ? { ...j, isSaved } : j));
            return { ...prev, content: updated };
          });
        }}
      />
    </div>
  );
};
