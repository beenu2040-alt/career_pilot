import React, { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import {
  Compass,
  CheckCircle2,
  Circle,
  Clock,
  ExternalLink,
  Code2,
  CheckSquare,
  Sparkles,
  RefreshCw,
  Trash2,
  Target,
  Award,
  TrendingUp,
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  BookOpen,
  ShieldCheck,
  Zap,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import {
  fetchLatestCareerRoadmap,
  fetchCareerRoadmaps,
  fetchCareerRoadmapById,
  generateCareerRoadmap,
  updateMilestoneStatus,
  deleteCareerRoadmap,
  fetchUserProfile,
  type RoadmapDetail,
  type RoadmapSummary,
  type RoadmapMilestone,
  type ProfileResponse,
} from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";

export const RoadmapPage: React.FC = () => {
  const { session } = useAuth();
  const token = session?.access_token || "";

  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [roadmap, setRoadmap] = useState<RoadmapDetail | null>(null);
  const [roadmapsList, setRoadmapsList] = useState<RoadmapSummary[]>([]);
  const [error, setError] = useState<string | null>(null);

  // Milestone expansion state
  const [expandedMilestones, setExpandedMilestones] = useState<Record<string, boolean>>({});

  // Generate / Regenerate Modal
  const [isGenerateModalOpen, setIsGenerateModalOpen] = useState(false);
  const [targetRoleInput, setTargetRoleInput] = useState("");
  const [seniorityLevelInput, setSeniorityLevelInput] = useState("Mid-Level");
  const [generating, setGenerating] = useState(false);
  const [generateError, setGenerateError] = useState<string | null>(null);

  // Updating milestone state
  const [updatingMilestoneId, setUpdatingMilestoneId] = useState<string | null>(null);

  // Notes editing state per milestone
  const [notesState, setNotesState] = useState<Record<string, string>>({});
  const [savingNotesId, setSavingNotesId] = useState<string | null>(null);

  // Delete modal
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // Active skill tab
  const [skillTab, setSkillTab] = useState<"gaps" | "acquired">("gaps");

  const loadData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);

    try {
      const [profileData, latestRoadmap, summaries] = await Promise.all([
        fetchUserProfile(token).catch(() => null),
        fetchLatestCareerRoadmap(token),
        fetchCareerRoadmaps(token).catch(() => []),
      ]);

      setProfile(profileData);
      setRoadmap(latestRoadmap);
      setRoadmapsList(summaries);

      if (profileData?.targetRole) {
        setTargetRoleInput(profileData.targetRole);
      }

      // Initialize milestone notes and expansion (first milestone open by default)
      if (latestRoadmap?.milestones) {
        const initialNotes: Record<string, string> = {};
        const initialExpanded: Record<string, boolean> = {};

        latestRoadmap.milestones.forEach((m: RoadmapMilestone, idx: number) => {
          initialNotes[m.id] = m.notes || "";
          initialExpanded[m.id] = idx === 0 || m.status === "IN_PROGRESS";
        });

        setNotesState(initialNotes);
        setExpandedMilestones(initialExpanded);
      }
    } catch (err: any) {
      setError(err.message || "Failed to load career roadmap data.");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleSelectRoadmap = async (id: string) => {
    if (!token) return;
    setLoading(true);
    try {
      const selected = await fetchCareerRoadmapById(id, token);
      setRoadmap(selected);

      const notes: Record<string, string> = {};
      const expanded: Record<string, boolean> = {};
      selected.milestones?.forEach((m: RoadmapMilestone, idx: number) => {
        notes[m.id] = m.notes || "";
        expanded[m.id] = idx === 0 || m.status === "IN_PROGRESS";
      });
      setNotesState(notes);
      setExpandedMilestones(expanded);
    } catch (err: any) {
      setError(err.message || "Failed to switch roadmap.");
    } finally {
      setLoading(false);
    }
  };

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;

    const role = targetRoleInput.trim();
    if (!role) {
      setGenerateError("Please enter a target role.");
      return;
    }

    setGenerating(true);
    setGenerateError(null);

    try {
      const generated = await generateCareerRoadmap(
        {
          targetRole: role,
          currentLevel: seniorityLevelInput,
        },
        token
      );

      setRoadmap(generated);
      setIsGenerateModalOpen(false);

      // Refresh list
      const summaries = await fetchCareerRoadmaps(token).catch(() => []);
      setRoadmapsList(summaries);

      const notes: Record<string, string> = {};
      const expanded: Record<string, boolean> = {};
      generated.milestones?.forEach((m: RoadmapMilestone, idx: number) => {
        notes[m.id] = m.notes || "";
        expanded[m.id] = idx === 0 || m.status === "IN_PROGRESS";
      });
      setNotesState(notes);
      setExpandedMilestones(expanded);
    } catch (err: any) {
      setGenerateError(err.message || "Failed to generate roadmap.");
    } finally {
      setGenerating(false);
    }
  };

  const handleStatusChange = async (
    milestone: RoadmapMilestone,
    newStatus: "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED"
  ) => {
    if (!token || !roadmap || milestone.status === newStatus) return;

    setUpdatingMilestoneId(milestone.id);
    try {
      const updated = await updateMilestoneStatus(
        roadmap.id,
        milestone.id,
        {
          status: newStatus,
          notes: notesState[milestone.id],
        },
        token
      );

      setRoadmap(updated);
    } catch (err: any) {
      alert("Failed to update milestone status: " + (err.message || "Unknown error"));
    } finally {
      setUpdatingMilestoneId(null);
    }
  };

  const handleSaveNotes = async (milestoneId: string) => {
    if (!token || !roadmap) return;
    const currentMilestone = roadmap.milestones.find((m) => m.id === milestoneId);
    if (!currentMilestone) return;

    setSavingNotesId(milestoneId);
    try {
      const updated = await updateMilestoneStatus(
        roadmap.id,
        milestoneId,
        {
          status: currentMilestone.status,
          notes: notesState[milestoneId],
        },
        token
      );
      setRoadmap(updated);
    } catch (err: any) {
      alert("Failed to save notes: " + (err.message || "Unknown error"));
    } finally {
      setSavingNotesId(null);
    }
  };

  const handleDelete = async () => {
    if (!token || !roadmap) return;
    setDeleting(true);
    try {
      await deleteCareerRoadmap(roadmap.id, token);
      setIsDeleteModalOpen(false);
      await loadData();
    } catch (err: any) {
      alert("Failed to delete roadmap: " + (err.message || "Unknown error"));
    } finally {
      setDeleting(false);
    }
  };

  const toggleMilestoneExpand = (milestoneId: string) => {
    setExpandedMilestones((prev) => ({
      ...prev,
      [milestoneId]: !prev[milestoneId],
    }));
  };

  if (loading && !roadmap) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="space-y-2">
            <Skeleton className="h-8 w-64" />
            <Skeleton className="h-4 w-96" />
          </div>
          <Skeleton className="h-10 w-36" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Skeleton className="h-36" />
          <Skeleton className="h-36" />
          <Skeleton className="h-36" />
        </div>
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header and Action Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200/80 pb-6 dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-violet-50 text-violet-700 dark:bg-violet-950/50 dark:text-violet-300 border border-violet-200/60 dark:border-violet-800/50">
              <Compass className="h-3.5 w-3.5" />
              Career Roadmap
            </span>
            {roadmap && (
              <Badge variant="outline" className="text-xs font-medium">
                {roadmap.currentLevel || "Mid-Level"}
              </Badge>
            )}
            {roadmap?.status === "COMPLETED" && (
              <Badge variant="success" className="text-xs">
                Completed
              </Badge>
            )}
            {roadmap?.status === "IN_PROGRESS" && (
              <Badge variant="info" className="text-xs">
                In Progress
              </Badge>
            )}
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
            {roadmap ? `${roadmap.targetRole} Roadmap` : "Personalized Career Roadmap"}
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1 max-w-2xl">
            {roadmap?.summary ||
              "Strategic skill progression plan generated from your resume, profile competencies, and live market requisition benchmarks."}
          </p>
        </div>

        <div className="flex items-center gap-3">
          {roadmapsList.length > 1 && (
            <div className="relative">
              <select
                aria-label="Select previous career roadmap version"
                value={roadmap?.id || ""}
                onChange={(e) => handleSelectRoadmap(e.target.value)}
                className="h-9 text-xs rounded-lg border border-slate-200 bg-white px-3 py-1 font-medium text-slate-700 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-200"
              >
                {roadmapsList.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.targetRole} ({r.overallProgress}% -{" "}
                    {new Date(r.createdAt).toLocaleDateString()})
                  </option>
                ))}
              </select>
            </div>
          )}

          <Button
            onClick={() => {
              if (profile?.targetRole) {
                setTargetRoleInput(profile.targetRole);
              }
              setIsGenerateModalOpen(true);
            }}
            className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm"
          >
            <Sparkles className="h-4 w-4" />
            {roadmap ? "Regenerate Plan" : "Generate Roadmap"}
          </Button>

          {roadmap && (
            <Button
              variant="outline"
              size="icon"
              onClick={() => setIsDeleteModalOpen(true)}
              className="text-slate-400 hover:text-rose-600 dark:hover:text-rose-400"
              title="Delete roadmap"
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl border border-rose-200 bg-rose-50/70 text-rose-800 dark:border-rose-900/40 dark:bg-rose-950/30 dark:text-rose-300 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <AlertTriangle className="h-5 w-5 text-rose-600 shrink-0" />
            <p className="text-sm font-medium">{error}</p>
          </div>
          <Button variant="outline" size="sm" onClick={loadData}>
            Retry
          </Button>
        </div>
      )}

      {/* Main Roadmap Content or Empty State */}
      {!roadmap ? (
        <div className="py-12">
          <EmptyState
            icon={<Compass className="h-8 w-8 text-indigo-600" />}
            title="No Career Roadmap Generated Yet"
            description={
              profile?.targetRole
                ? `You have set your target role to "${profile.targetRole}". Generate your personalized roadmap to calculate market skill gaps and actionable learning milestones.`
                : "Generate a custom milestone roadmap to evaluate your skills against active market job postings and track your progress to role readiness."
            }
            actionText="Generate Roadmap"
            onAction={() => {
              if (profile?.targetRole) {
                setTargetRoleInput(profile.targetRole);
              }
              setIsGenerateModalOpen(true);
            }}
          />
          {!profile?.targetRole && (
            <p className="text-center text-xs text-slate-400 mt-4">
              Tip: You can also specify your target role directly in{" "}
              <Link to="/profile" className="text-indigo-600 hover:underline">
                My Profile
              </Link>
              .
            </p>
          )}
        </div>
      ) : (
        <div className="space-y-8">
          {/* Key Metrics Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {/* Progress Card */}
            <div className="p-5 rounded-2xl border border-slate-200/80 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                  Overall Completion
                </span>
                <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600 dark:bg-indigo-950/50 dark:text-indigo-400">
                  <TrendingUp className="h-4 w-4" />
                </span>
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-3xl font-extrabold text-slate-900 dark:text-slate-100">
                  {roadmap.overallProgress}%
                </span>
                <span className="text-xs text-slate-500">
                  ({roadmap.milestones.filter((m) => m.status === "COMPLETED").length} of{" "}
                  {roadmap.milestones.length} milestones complete)
                </span>
              </div>
              {/* Progress Bar */}
              <div className="w-full bg-slate-100 rounded-full h-2.5 mt-3 overflow-hidden dark:bg-slate-800">
                <div
                  className="bg-indigo-600 h-2.5 rounded-full transition-all duration-500 ease-out"
                  style={{ width: `${roadmap.overallProgress}%` }}
                />
              </div>
            </div>

            {/* Target Role Readiness Card */}
            <div className="p-5 rounded-2xl border border-slate-200/80 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                  Role Readiness Score
                </span>
                <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-950/50 dark:text-emerald-400">
                  <Award className="h-4 w-4" />
                </span>
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-3xl font-extrabold text-slate-900 dark:text-slate-100">
                  {roadmap.skillGapSummary?.readinessScore ?? 65}%
                </span>
                <span className="text-xs text-slate-500">
                  based on market job requisitions
                </span>
              </div>
              <div className="w-full bg-slate-100 rounded-full h-2.5 mt-3 overflow-hidden dark:bg-slate-800">
                <div
                  className="bg-emerald-600 h-2.5 rounded-full transition-all duration-500 ease-out"
                  style={{
                    width: `${roadmap.skillGapSummary?.readinessScore ?? 65}%`,
                  }}
                />
              </div>
            </div>

            {/* Target Skill Ratio Card */}
            <div className="p-5 rounded-2xl border border-slate-200/80 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                  Identified Skill Gaps
                </span>
                <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-50 text-amber-600 dark:bg-amber-950/50 dark:text-amber-400">
                  <Target className="h-4 w-4" />
                </span>
              </div>
              <div className="flex items-baseline gap-2">
                <span className="text-3xl font-extrabold text-slate-900 dark:text-slate-100">
                  {roadmap.skillGapSummary?.gapSkillsCount ?? 0}
                </span>
                <span className="text-xs text-slate-500">
                  to acquire out of {roadmap.skillGapSummary?.totalTargetSkills ?? 0} market skills
                </span>
              </div>
              <div className="flex items-center gap-2 mt-3 text-xs text-slate-500">
                <span className="font-semibold text-emerald-600">
                  {roadmap.skillGapSummary?.acquiredSkillsCount ?? 0} Acquired
                </span>
                <span>•</span>
                <span className="font-semibold text-amber-600">
                  {roadmap.skillGapSummary?.gapSkillsCount ?? 0} Priority Gaps
                </span>
              </div>
            </div>
          </div>

          {/* Skill Gap Analysis Breakdown */}
          {roadmap.skillGapSummary && (
            <div className="rounded-2xl border border-slate-200/80 bg-white shadow-sm overflow-hidden dark:border-slate-800 dark:bg-slate-900">
              <div className="p-5 border-b border-slate-100 dark:border-slate-800/80 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                  <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                    <ShieldCheck className="h-5 w-5 text-indigo-600" />
                    Market Requisition Skill Comparison
                  </h2>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                    Deterministic comparison between your profile/resume evidence and requirements in active jobs.
                  </p>
                </div>
                {/* Tabs */}
                <div className="flex items-center p-1 rounded-xl bg-slate-100 dark:bg-slate-800 text-xs font-medium">
                  <button
                    onClick={() => setSkillTab("gaps")}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${
                      skillTab === "gaps"
                        ? "bg-white text-slate-900 shadow-sm dark:bg-slate-700 dark:text-slate-100"
                        : "text-slate-500 hover:text-slate-900 dark:hover:text-slate-200"
                    }`}
                  >
                    Priority Gaps ({roadmap.skillGapSummary.gapSkills.length})
                  </button>
                  <button
                    onClick={() => setSkillTab("acquired")}
                    className={`px-3 py-1.5 rounded-lg transition-colors ${
                      skillTab === "acquired"
                        ? "bg-white text-slate-900 shadow-sm dark:bg-slate-700 dark:text-slate-100"
                        : "text-slate-500 hover:text-slate-900 dark:hover:text-slate-200"
                    }`}
                  >
                    Acquired & Verified ({roadmap.skillGapSummary.acquiredSkills.length})
                  </button>
                </div>
              </div>

              <div className="p-5">
                {skillTab === "gaps" ? (
                  roadmap.skillGapSummary.gapSkills.length === 0 ? (
                    <p className="text-xs text-slate-500 py-4 text-center">
                      No skill gaps identified! You match all requirements in the job catalog for {roadmap.targetRole}.
                    </p>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                      {roadmap.skillGapSummary.gapSkills.map((gap, i) => (
                        <div
                          key={i}
                          className="p-3.5 rounded-xl border border-amber-200/70 bg-amber-50/40 dark:border-amber-900/40 dark:bg-amber-950/20"
                        >
                          <div className="flex items-center justify-between mb-1.5">
                            <span className="font-semibold text-sm text-slate-900 dark:text-slate-100">
                              {gap.skill}
                            </span>
                            <Badge
                              variant={gap.priority === "HIGH" ? "warning" : "secondary"}
                              className="text-[10px] uppercase font-bold"
                            >
                              {gap.priority}
                            </Badge>
                          </div>
                          <div className="text-xs text-slate-500 dark:text-slate-400 space-y-1">
                            <div className="flex items-center gap-1.5 text-amber-700 dark:text-amber-400">
                              <Zap className="h-3.5 w-3.5 shrink-0" />
                              <span className="font-medium">{gap.evidenceOrContext}</span>
                            </div>
                            <div className="text-[11px] text-slate-400">Category: {gap.category}</div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                    {roadmap.skillGapSummary.acquiredSkills.map((acq, i) => (
                      <div
                        key={i}
                        className="p-3.5 rounded-xl border border-emerald-200/70 bg-emerald-50/30 dark:border-emerald-900/40 dark:bg-emerald-950/20"
                      >
                        <div className="flex items-center justify-between mb-1.5">
                          <span className="font-semibold text-sm text-slate-900 dark:text-slate-100">
                            {acq.skill}
                          </span>
                          <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-700 bg-emerald-100/70 px-2 py-0.5 rounded-full dark:bg-emerald-950 dark:text-emerald-300">
                            <CheckCircle2 className="h-3 w-3" />
                            Verified
                          </span>
                        </div>
                        <div className="text-xs text-slate-500 dark:text-slate-400 space-y-1">
                          <p className="line-clamp-2 text-slate-600 dark:text-slate-300">
                            {acq.evidenceOrContext}
                          </p>
                          <div className="text-[11px] text-slate-400">Category: {acq.category}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Interactive Milestones Timeline */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                  <Compass className="h-5 w-5 text-indigo-600" />
                  Learning Milestones & Action Plan
                </h2>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  Follow each milestone sequentially. Mark milestones as In Progress or Completed as you advance.
                </p>
              </div>
              <div className="text-xs text-slate-500 font-medium">
                {roadmap.milestones.length} Strategic Phases
              </div>
            </div>

            <div className="space-y-5">
              {roadmap.milestones.map((milestone) => {
                const isExpanded = !!expandedMilestones[milestone.id];
                const isUpdating = updatingMilestoneId === milestone.id;

                return (
                  <div
                    key={milestone.id}
                    className={`rounded-2xl border transition-all duration-200 overflow-hidden ${
                      milestone.status === "COMPLETED"
                        ? "border-emerald-200/80 bg-white shadow-sm dark:border-emerald-950/60 dark:bg-slate-900"
                        : milestone.status === "IN_PROGRESS"
                        ? "border-indigo-300 bg-white shadow-md ring-1 ring-indigo-500/20 dark:border-indigo-800 dark:bg-slate-900"
                        : "border-slate-200/80 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900"
                    }`}
                  >
                    {/* Milestone Header Banner */}
                    <div className="p-5 sm:p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
                      <div className="flex items-start gap-4">
                        {/* Status Marker Icon */}
                        <div className="mt-0.5">
                          {milestone.status === "COMPLETED" ? (
                            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-400">
                              <CheckCircle2 className="h-5 w-5" />
                            </span>
                          ) : milestone.status === "IN_PROGRESS" ? (
                            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-100 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400 animate-pulse">
                              <Compass className="h-5 w-5" />
                            </span>
                          ) : (
                            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-400 dark:bg-slate-800">
                              <Circle className="h-5 w-5" />
                            </span>
                          )}
                        </div>

                        <div>
                          <div className="flex items-center gap-2 flex-wrap mb-1">
                            <span className="text-xs font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
                              Milestone {milestone.sequenceOrder}
                            </span>
                            <span className="text-slate-300 dark:text-slate-700">•</span>
                            <span className="flex items-center gap-1 text-xs text-slate-500">
                              <Clock className="h-3.5 w-3.5" />
                              {milestone.estimatedDurationWeeks} Weeks Estimated
                            </span>
                            {milestone.completedAt && (
                              <>
                                <span className="text-slate-300 dark:text-slate-700">•</span>
                                <span className="text-xs text-emerald-600 font-medium">
                                  Completed {new Date(milestone.completedAt).toLocaleDateString()}
                                </span>
                              </>
                            )}
                          </div>
                          <h3 className="text-base sm:text-lg font-bold text-slate-900 dark:text-slate-100">
                            {milestone.title}
                          </h3>
                          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400 mt-1 max-w-2xl">
                            {milestone.description}
                          </p>
                        </div>
                      </div>

                      {/* Status Selector & Expand Toggle */}
                      <div className="flex items-center gap-3 self-end md:self-center shrink-0">
                        {/* Status switcher pills */}
                        <div className="inline-flex rounded-xl p-1 bg-slate-100 dark:bg-slate-800 text-xs font-medium">
                          <button
                            disabled={isUpdating}
                            onClick={() => handleStatusChange(milestone, "NOT_STARTED")}
                            className={`px-2.5 py-1 rounded-lg transition-colors ${
                              milestone.status === "NOT_STARTED"
                                ? "bg-white text-slate-800 shadow-sm font-semibold dark:bg-slate-700 dark:text-slate-100"
                                : "text-slate-500 hover:text-slate-800 dark:hover:text-slate-300"
                            }`}
                          >
                            To Do
                          </button>
                          <button
                            disabled={isUpdating}
                            onClick={() => handleStatusChange(milestone, "IN_PROGRESS")}
                            className={`px-2.5 py-1 rounded-lg transition-colors ${
                              milestone.status === "IN_PROGRESS"
                                ? "bg-indigo-600 text-white shadow-sm font-semibold"
                                : "text-slate-500 hover:text-slate-800 dark:hover:text-slate-300"
                            }`}
                          >
                            In Progress
                          </button>
                          <button
                            disabled={isUpdating}
                            onClick={() => handleStatusChange(milestone, "COMPLETED")}
                            className={`px-2.5 py-1 rounded-lg transition-colors ${
                              milestone.status === "COMPLETED"
                                ? "bg-emerald-600 text-white shadow-sm font-semibold"
                                : "text-slate-500 hover:text-slate-800 dark:hover:text-slate-300"
                            }`}
                          >
                            Completed
                          </button>
                        </div>

                        {/* Expand / Collapse Button */}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => toggleMilestoneExpand(milestone.id)}
                          className="flex items-center gap-1 text-slate-500 hover:text-slate-900 dark:hover:text-slate-100"
                        >
                          <span className="text-xs">{isExpanded ? "Less" : "Details"}</span>
                          {isExpanded ? (
                            <ChevronUp className="h-4 w-4" />
                          ) : (
                            <ChevronDown className="h-4 w-4" />
                          )}
                        </Button>
                      </div>
                    </div>

                    {/* Expandable Milestone Details Panel */}
                    {isExpanded && (
                      <div className="border-t border-slate-100 p-5 sm:p-6 bg-slate-50/50 space-y-6 dark:border-slate-800/80 dark:bg-slate-900/40">
                        {/* 1. Skills Covered */}
                        {milestone.skills && milestone.skills.length > 0 && (
                          <div>
                            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2.5 flex items-center gap-1.5">
                              <Target className="h-3.5 w-3.5 text-indigo-500" />
                              Target Competencies in this Phase
                            </h4>
                            <div className="flex flex-wrap gap-2">
                              {milestone.skills.map((skill, sIdx) => (
                                <div
                                  key={sIdx}
                                  className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg border border-slate-200 bg-white text-xs text-slate-700 shadow-sm dark:border-slate-800 dark:bg-slate-800 dark:text-slate-200"
                                >
                                  <span className="font-semibold">{skill.name}</span>
                                  <Badge
                                    variant={skill.priority === "HIGH" ? "default" : "secondary"}
                                    className="text-[9px] py-0 px-1.5 uppercase"
                                  >
                                    {skill.priority}
                                  </Badge>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* 2. Practical Projects */}
                        {milestone.projects && milestone.projects.length > 0 && (
                          <div>
                            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2.5 flex items-center gap-1.5">
                              <Code2 className="h-3.5 w-3.5 text-indigo-500" />
                              Hands-on Project & Deliverables
                            </h4>
                            <div className="space-y-3">
                              {milestone.projects.map((proj, pIdx) => (
                                <div
                                  key={pIdx}
                                  className="p-4 rounded-xl border border-slate-200/90 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-800/60"
                                >
                                  <h5 className="font-bold text-sm text-slate-900 dark:text-slate-100">
                                    {proj.title}
                                  </h5>
                                  <p className="text-xs text-slate-600 dark:text-slate-400 mt-1">
                                    {proj.description}
                                  </p>
                                  <div className="mt-3 p-2.5 rounded-lg bg-indigo-50/60 border border-indigo-100 text-xs text-indigo-900 dark:bg-indigo-950/30 dark:border-indigo-900/50 dark:text-indigo-200">
                                    <span className="font-semibold">Deliverable: </span>
                                    {proj.deliverable}
                                  </div>
                                  {proj.techStack && proj.techStack.length > 0 && (
                                    <div className="flex flex-wrap gap-1.5 mt-2.5">
                                      {proj.techStack.map((tech, tIdx) => (
                                        <span
                                          key={tIdx}
                                          className="text-[10px] font-medium px-2 py-0.5 rounded bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300"
                                        >
                                          {tech}
                                        </span>
                                      ))}
                                    </div>
                                  )}
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* 3. Verified Learning Resources */}
                        {milestone.resources && milestone.resources.length > 0 && (
                          <div>
                            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2.5 flex items-center gap-1.5">
                              <BookOpen className="h-3.5 w-3.5 text-indigo-500" />
                              Verified Learning Resources
                            </h4>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                              {milestone.resources.map((res, rIdx) => (
                                <a
                                  key={rIdx}
                                  href={res.url}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="group flex items-center justify-between p-3 rounded-xl border border-slate-200/80 bg-white hover:border-indigo-300 hover:shadow-sm transition-all dark:border-slate-800 dark:bg-slate-800/80 dark:hover:border-indigo-700"
                                >
                                  <div className="flex items-center gap-2.5 overflow-hidden">
                                    <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600 shrink-0 group-hover:bg-indigo-600 group-hover:text-white transition-colors dark:bg-indigo-950 dark:text-indigo-400">
                                      <BookOpen className="h-3.5 w-3.5" />
                                    </span>
                                    <div className="overflow-hidden">
                                      <p className="text-xs font-medium text-slate-900 group-hover:text-indigo-600 truncate transition-colors dark:text-slate-100 dark:group-hover:text-indigo-400">
                                        {res.title}
                                      </p>
                                      <span className="text-[10px] text-slate-400 uppercase tracking-wider font-semibold">
                                        {res.type}
                                      </span>
                                    </div>
                                  </div>
                                  <ExternalLink className="h-3.5 w-3.5 text-slate-400 group-hover:text-indigo-600 shrink-0 transition-colors ml-2" />
                                </a>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* 4. Completion Criteria */}
                        {milestone.completionCriteria && milestone.completionCriteria.length > 0 && (
                          <div>
                            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
                              <CheckSquare className="h-3.5 w-3.5 text-indigo-500" />
                              Milestone Verification Criteria
                            </h4>
                            <ul className="space-y-1.5">
                              {milestone.completionCriteria.map((criterion, cIdx) => (
                                <li
                                  key={cIdx}
                                  className="flex items-start gap-2 text-xs text-slate-700 dark:text-slate-300"
                                >
                                  <span className="mt-0.5 text-emerald-600 dark:text-emerald-400">
                                    ✓
                                  </span>
                                  <span>{criterion}</span>
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}

                        {/* 5. Notes & Reflection */}
                        <div>
                          <div className="flex items-center justify-between mb-1.5">
                            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                              Personal Notes & Artifacts
                            </h4>
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={savingNotesId === milestone.id}
                              onClick={() => handleSaveNotes(milestone.id)}
                              className="text-xs h-7 px-2.5"
                            >
                              {savingNotesId === milestone.id ? "Saving..." : "Save Notes"}
                            </Button>
                          </div>
                          <textarea
                            value={notesState[milestone.id] || ""}
                            onChange={(e) =>
                              setNotesState((prev) => ({
                                ...prev,
                                [milestone.id]: e.target.value,
                              }))
                            }
                            placeholder="Add links to your repository, notes on concepts mastered, or project reflections..."
                            className="w-full text-xs p-3 rounded-xl border border-slate-200 bg-white placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 dark:border-slate-800 dark:bg-slate-800 dark:text-slate-200 min-h-[75px]"
                          />
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Generate / Customize Roadmap Dialog */}
      <Dialog
        open={isGenerateModalOpen}
        onOpenChange={setIsGenerateModalOpen}
        title={roadmap ? "Regenerate Career Roadmap" : "Generate Personalized Career Roadmap"}
        description="Specify your target career goal and seniority tier. The AI engine synthesizes job catalog requirements and your profile skills into a phased curriculum."
      >
        <form onSubmit={handleGenerate} className="space-y-4 pt-2">
          {generateError && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-700 dark:bg-rose-950/40 dark:border-rose-900 dark:text-rose-300">
              {generateError}
            </div>
          )}

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
              Target Role <span className="text-rose-500">*</span>
            </label>
            <Input
              type="text"
              value={targetRoleInput}
              onChange={(e) => setTargetRoleInput(e.target.value)}
              placeholder="e.g. Senior Backend Engineer, Lead Cloud Architect"
              required
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
              Target Seniority Tier
            </label>
            <select
              value={seniorityLevelInput}
              onChange={(e) => setSeniorityLevelInput(e.target.value)}
              className="w-full h-10 text-sm rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-800 shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-200"
            >
              <option value="Junior">Junior Engineer (0-2 years)</option>
              <option value="Mid-Level">Mid-Level Engineer (2-5 years)</option>
              <option value="Senior">Senior Engineer (5-8 years)</option>
              <option value="Staff / Lead">Staff / Principal Architect (8+ years)</option>
            </select>
          </div>

          <div className="pt-4 flex items-center justify-end gap-2 border-t border-slate-100 dark:border-slate-800">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsGenerateModalOpen(false)}
              disabled={generating}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={generating}
              className="bg-indigo-600 hover:bg-indigo-700 text-white flex items-center gap-2"
            >
              {generating && <RefreshCw className="h-4 w-4 animate-spin" />}
              {generating ? "Synthesizing Roadmap..." : "Generate Roadmap"}
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={isDeleteModalOpen}
        onOpenChange={setIsDeleteModalOpen}
        title="Delete Career Roadmap"
        description="Are you sure you want to delete this career roadmap? This will permanently remove all tracked milestones, progress, and personal notes."
      >
        <div className="pt-4 flex items-center justify-end gap-2 border-t border-slate-100 dark:border-slate-800">
          <Button
            type="button"
            variant="outline"
            onClick={() => setIsDeleteModalOpen(false)}
            disabled={deleting}
          >
            Cancel
          </Button>
          <Button
            type="button"
            variant="destructive"
            onClick={handleDelete}
            disabled={deleting}
            className="flex items-center gap-1.5"
          >
            {deleting && <RefreshCw className="h-4 w-4 animate-spin" />}
            {deleting ? "Deleting..." : "Delete Plan"}
          </Button>
        </div>
      </Dialog>
    </div>
  );
};
