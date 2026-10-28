import React, { useState, useEffect } from "react";
import { useAuth } from "@/context/AuthContext";
import { Link } from "react-router-dom";
import {
  Wand2,
  FileText,
  Sparkles,
  CheckCircle2,
  XCircle,
  Edit3,
  Download,
  Copy,
  AlertTriangle,
  AlertCircle,
  ShieldCheck,
  RefreshCw,
  Plus,
  History,
  Info,
  Check,
} from "lucide-react";
import {
  ResumeSummary,
  JobSummaryResponse,
  TailoringSession,
  TailoringSessionDetail,
  TailoredResumeVersion,
  SuggestionSection,
  fetchUserResumes,
  fetchJobs,
  fetchSavedJobs,
  fetchTailoringSessions,
  fetchTailoringSessionById,
  createTailoringSession,
  generateTailoringSuggestions,
  updateTailoringSuggestion,
  saveTailoredVersion,
  exportTailoredResumePdf,
  deleteTailoringSession,
} from "@/lib/api";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export const ResumeTailorPage: React.FC = () => {
  const { accessToken } = useAuth();

  // State: Source data
  const [resumes, setResumes] = useState<ResumeSummary[]>([]);
  const [availableJobs, setAvailableJobs] = useState<JobSummaryResponse[]>([]);
  const [sessions, setSessions] = useState<TailoringSession[]>([]);

  // State: Active selection
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [currentSession, setCurrentSession] = useState<TailoringSessionDetail | null>(null);
  const [activeVersion, setActiveVersion] = useState<TailoredResumeVersion | null>(null);

  // State: Setup form (New Session)
  const [showNewSessionModal, setShowNewSessionModal] = useState(false);
  const [selectedResumeId, setSelectedResumeId] = useState<string>("");
  const [jobMode, setJobMode] = useState<"stored" | "manual">("stored");
  const [selectedJobId, setSelectedJobId] = useState<string>("");
  const [customJobTitle, setCustomJobTitle] = useState("");
  const [customCompanyName, setCustomCompanyName] = useState("");
  const [customJobDescription, setCustomJobDescription] = useState("");

  // State: UI status
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isSavingVersion, setIsSavingVersion] = useState(false);
  const [isExportingPdf, setIsExportingPdf] = useState(false);
  const [copiedText, setCopiedText] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // State: Editing suggestion inline
  const [editingSuggestionId, setEditingSuggestionId] = useState<string | null>(null);
  const [editedText, setEditedText] = useState<string>("");

  // State: Filter suggestions section
  const [filterSection, setFilterSection] = useState<SuggestionSection | "ALL">("ALL");

  // State: Active tab in workspace
  const [activeTab, setActiveTab] = useState<"suggestions" | "preview" | "compare">("suggestions");

  useEffect(() => {
    if (accessToken) {
      loadInitialData();
    }
  }, [accessToken]);

  const loadInitialData = async () => {
    if (!accessToken) return;
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [resumesRes, jobsRes, savedJobsRes, sessionsRes] = await Promise.all([
        fetchUserResumes(accessToken).catch(() => []),
        fetchJobs({ page: 0, size: 20 }, accessToken).catch(() => ({ content: [] })),
        fetchSavedJobs(accessToken, 0, 20).catch(() => null),
        fetchTailoringSessions(accessToken).catch(() => []),
      ]);

      setResumes(resumesRes);
      if (resumesRes.length > 0 && !selectedResumeId) {
        setSelectedResumeId(resumesRes[0].id);
      }

      const mergedJobs = new Map<string, JobSummaryResponse>();
      if (savedJobsRes?.content) {
        savedJobsRes.content.forEach((j: JobSummaryResponse) => mergedJobs.set(j.id, j));
      }
      if (jobsRes?.content) {
        jobsRes.content.forEach((j: JobSummaryResponse) => {
          if (!mergedJobs.has(j.id)) mergedJobs.set(j.id, j);
        });
      }
      const jobList = Array.from(mergedJobs.values());
      setAvailableJobs(jobList);
      if (jobList.length > 0 && !selectedJobId) {
        setSelectedJobId(jobList[0].id);
      }

      setSessions(sessionsRes);
      if (sessionsRes.length > 0 && !selectedSessionId) {
        loadSessionDetail(sessionsRes[0].id);
      }
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to load initial tailoring workspace.");
    } finally {
      setIsLoading(false);
    }
  };

  const loadSessionDetail = async (sessionId: string) => {
    if (!accessToken) return;
    try {
      const detail = await fetchTailoringSessionById(sessionId, accessToken);
      setSelectedSessionId(sessionId);
      setCurrentSession(detail);
      if (detail.versions && detail.versions.length > 0) {
        setActiveVersion(detail.versions[0]);
      } else {
        setActiveVersion(null);
      }
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to load session details.");
    }
  };

  const handleCreateSession = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accessToken) return;
    if (!selectedResumeId) {
      setErrorMessage("Please select a source resume.");
      return;
    }
    if (jobMode === "stored" && !selectedJobId) {
      setErrorMessage("Please select a target job from the database.");
      return;
    }
    if (jobMode === "manual" && !customJobDescription.trim()) {
      setErrorMessage("Please enter the job description requirements.");
      return;
    }

    setIsCreating(true);
    setErrorMessage(null);
    try {
      const payload = {
        resumeId: selectedResumeId,
        jobId: jobMode === "stored" ? selectedJobId : undefined,
        customJobTitle: jobMode === "manual" ? customJobTitle : undefined,
        customCompanyName: jobMode === "manual" ? customCompanyName : undefined,
        customJobDescription: jobMode === "manual" ? customJobDescription : undefined,
      };

      const newSession = await createTailoringSession(payload, accessToken);
      setShowNewSessionModal(false);
      setCustomJobTitle("");
      setCustomCompanyName("");
      setCustomJobDescription("");

      // Automatically generate initial suggestions
      setIsGenerating(true);
      const generatedDetail = await generateTailoringSuggestions(newSession.id, accessToken);
      setCurrentSession(generatedDetail);
      setSelectedSessionId(newSession.id);
      setActiveVersion(null);

      // Refresh session list
      const updatedSessions = await fetchTailoringSessions(accessToken);
      setSessions(updatedSessions);
      setSuccessMessage("Resume tailoring session initialized and suggestions generated!");
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to create tailoring session.");
    } finally {
      setIsCreating(false);
      setIsGenerating(false);
    }
  };

  const handleRegenerateSuggestions = async () => {
    if (!accessToken || !selectedSessionId) return;
    setIsGenerating(true);
    setErrorMessage(null);
    try {
      const updated = await generateTailoringSuggestions(selectedSessionId, accessToken);
      setCurrentSession(updated);
      setSuccessMessage("Generated fresh tailoring suggestions based on job requirements.");
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to regenerate suggestions.");
    } finally {
      setIsGenerating(false);
    }
  };

  const handleSuggestionDecision = async (
    suggestionId: string,
    status: "ACCEPTED" | "REJECTED" | "PENDING" | "EDITED",
    userContent?: string
  ) => {
    if (!accessToken || !selectedSessionId) return;
    try {
      const updated = await updateTailoringSuggestion(
        selectedSessionId,
        suggestionId,
        { status, userEditedContent: userContent },
        accessToken
      );

      // Update in currentSession
      if (currentSession) {
        const updatedSuggestions = currentSession.suggestions.map((s) =>
          s.id === suggestionId ? updated : s
        );
        setCurrentSession({ ...currentSession, suggestions: updatedSuggestions });
      }
      setEditingSuggestionId(null);
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to update suggestion status.");
    }
  };

  const handleBatchAcceptSafe = async () => {
    if (!currentSession || !accessToken || !selectedSessionId) return;
    const safePending = currentSession.suggestions.filter(
      (s) => s.status === "PENDING" && !s.requiresUserConfirmation
    );
    for (const s of safePending) {
      await handleSuggestionDecision(s.id, "ACCEPTED");
    }
    setSuccessMessage(`Accepted ${safePending.length} verified suggestions.`);
    setTimeout(() => setSuccessMessage(null), 3000);
  };

  const handleSaveVersion = async () => {
    if (!accessToken || !selectedSessionId || !currentSession) return;
    setIsSavingVersion(true);
    setErrorMessage(null);
    try {
      const versionNum = (currentSession.versions?.length || 0) + 1;
      const versionName = `v${versionNum} - Tailored for ${currentSession.jobTitle} at ${currentSession.companyName || "Target"}`;
      const newVersion = await saveTailoredVersion(
        selectedSessionId,
        { versionName, changeSummary: "Applied tailored suggestions" },
        accessToken
      );

      // Update current session with new version
      const detail = await fetchTailoringSessionById(selectedSessionId, accessToken);
      setCurrentSession(detail);
      setActiveVersion(newVersion);
      setActiveTab("preview");
      setSuccessMessage(`Saved tailored version: ${versionName}!`);
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to save tailored version.");
    } finally {
      setIsSavingVersion(false);
    }
  };

  const handleDownloadPdf = async () => {
    if (!accessToken || !selectedSessionId || !activeVersion) return;
    setIsExportingPdf(true);
    try {
      const blob = await exportTailoredResumePdf(selectedSessionId, activeVersion.id, accessToken);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `tailored-resume-${currentSession?.jobTitle?.replace(/[^a-zA-Z0-9]/g, "-") || "custom"}-v${activeVersion.versionNumber}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to download PDF.");
    } finally {
      setIsExportingPdf(false);
    }
  };

  const handleCopyText = () => {
    if (!activeVersion?.tailoredPlainText) return;
    navigator.clipboard.writeText(activeVersion.tailoredPlainText);
    setCopiedText(true);
    setTimeout(() => setCopiedText(false), 2500);
  };

  const handleDeleteSession = async (sessionId: string) => {
    if (!accessToken) return;
    if (!window.confirm("Are you sure you want to delete this tailoring session?")) return;
    try {
      await deleteTailoringSession(sessionId, accessToken);
      const remaining = sessions.filter((s) => s.id !== sessionId);
      setSessions(remaining);
      if (selectedSessionId === sessionId) {
        if (remaining.length > 0) {
          loadSessionDetail(remaining[0].id);
        } else {
          setSelectedSessionId(null);
          setCurrentSession(null);
          setActiveVersion(null);
        }
      }
      setSuccessMessage("Session deleted successfully.");
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to delete session.");
    }
  };

  // Filtered suggestions
  const filteredSuggestions = currentSession?.suggestions.filter((s) =>
    filterSection === "ALL" ? true : s.section === filterSection
  ) || [];

  const acceptedCount = currentSession?.suggestions.filter((s) => s.status === "ACCEPTED" || s.status === "EDITED").length || 0;
  const pendingCount = currentSession?.suggestions.filter((s) => s.status === "PENDING").length || 0;
  const rejectedCount = currentSession?.suggestions.filter((s) => s.status === "REJECTED").length || 0;

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[500px] space-y-4">
        <RefreshCw className="h-8 w-8 animate-spin text-primary" />
        <p className="text-sm font-medium text-muted-foreground">Loading Resume Tailor workspace...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 1. HEADER BANNER */}
      <div className="rounded-2xl border border-slate-200/90 bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 p-6 sm:p-8 text-white shadow-md relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500/20 border border-amber-400/30 text-amber-300">
                <Wand2 className="h-5 w-5" />
              </span>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">AI Resume Tailoring</h1>
              <Badge variant="warning" className="text-xs bg-amber-500/20 text-amber-300 border-amber-400/30">
                Phase 6 Active
              </Badge>
            </div>
            <p className="text-sm text-slate-300 max-w-2xl leading-relaxed">
              Targeted resume optimization for specific job postings. Maximize ATS keyword alignment while preserving 100% strict factual accuracy.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Button
              onClick={() => setShowNewSessionModal(true)}
              className="bg-amber-500 hover:bg-amber-600 text-slate-950 font-semibold shadow-sm gap-2"
            >
              <Plus className="h-4 w-4" />
              New Tailoring Session
            </Button>
          </div>
        </div>
      </div>

      {/* NOTIFICATIONS */}
      {errorMessage && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-900 flex items-start gap-3">
          <AlertCircle className="h-5 w-5 text-red-600 mt-0.5 shrink-0" />
          <div className="text-sm">
            <p className="font-semibold">Action Required</p>
            <p className="text-red-800">{errorMessage}</p>
          </div>
        </div>
      )}

      {successMessage && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-900 flex items-start gap-3">
          <CheckCircle2 className="h-5 w-5 text-emerald-600 mt-0.5 shrink-0" />
          <div className="text-sm">
            <p className="font-semibold">Success</p>
            <p className="text-emerald-800">{successMessage}</p>
          </div>
        </div>
      )}

      {/* 2. MAIN WORKSPACE GRID */}
      {sessions.length === 0 && !showNewSessionModal ? (
        <Card className="border-dashed border-2 p-12 text-center">
          <CardContent className="space-y-4 max-w-md mx-auto">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-50 text-amber-600">
              <Wand2 className="h-7 w-7" />
            </div>
            <h3 className="text-lg font-bold text-slate-900">No Tailoring Sessions Yet</h3>
            <p className="text-sm text-muted-foreground">
              Select one of your uploaded resumes and target a job description from your bookmarks or paste any posting to start tailoring.
            </p>
            {resumes.length === 0 ? (
              <div className="pt-2">
                <Link to="/resume-analyzer">
                  <Button className="gap-2">
                    <FileText className="h-4 w-4" />
                    Upload Your First Resume
                  </Button>
                </Link>
              </div>
            ) : (
              <Button onClick={() => setShowNewSessionModal(true)} className="gap-2">
                <Plus className="h-4 w-4" />
                Create First Session
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* LEFT SIDEBAR: Sessions List */}
          <div className="lg:col-span-3 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-500">Tailoring Sessions</h2>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setShowNewSessionModal(true)}
                className="h-8 px-2 text-xs gap-1"
              >
                <Plus className="h-3.5 w-3.5" />
                New
              </Button>
            </div>

            <div className="space-y-2 max-h-[700px] overflow-y-auto pr-1">
              {sessions.map((s) => {
                const isSelected = s.id === selectedSessionId;
                return (
                  <div
                    key={s.id}
                    onClick={() => loadSessionDetail(s.id)}
                    className={`cursor-pointer rounded-xl border p-3.5 transition-all ${
                      isSelected
                        ? "border-primary bg-primary/5 shadow-sm ring-1 ring-primary/20"
                        : "border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/80"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="space-y-1 min-w-0">
                        <p className="text-sm font-semibold text-slate-900 truncate">{s.jobTitle}</p>
                        <p className="text-xs text-muted-foreground truncate">{s.companyName || "Target Employer"}</p>
                      </div>
                      <Badge
                        variant={s.status === "COMPLETED" ? "success" : "info"}
                        className="text-[10px] uppercase font-bold shrink-0"
                      >
                        {s.status === "COMPLETED" ? "Versioned" : "Ready"}
                      </Badge>
                    </div>

                    <div className="mt-3 flex items-center justify-between text-[11px] text-muted-foreground border-t pt-2">
                      <span className="truncate max-w-[120px]">{s.resumeFilename}</span>
                      <span className="font-semibold text-primary">{s.versionCount} versions</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* RIGHT WORKSPACE: Current Active Session */}
          <div className="lg:col-span-9 space-y-6">
            {currentSession && (
              <>
                {/* Session Target Summary Card */}
                <Card className="border-slate-200 bg-white shadow-sm">
                  <CardHeader className="p-5 pb-4 border-b border-slate-100">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div>
                        <div className="flex items-center gap-2">
                          <CardTitle className="text-xl font-bold text-slate-900">
                            {currentSession.jobTitle}
                          </CardTitle>
                          <Badge variant="outline" className="text-xs">
                            {currentSession.companyName || "Direct Posting"}
                          </Badge>
                        </div>
                        <CardDescription className="text-xs text-muted-foreground mt-1 flex items-center gap-2">
                          <span>Source Resume: <strong>{currentSession.resumeFilename}</strong></span>
                          <span>•</span>
                          <span>Model: <code className="bg-slate-100 px-1 py-0.5 rounded text-[11px]">{currentSession.modelIdentifier || "AI Tailoring Engine"}</code></span>
                        </CardDescription>
                      </div>

                      <div className="flex items-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={handleRegenerateSuggestions}
                          disabled={isGenerating}
                          className="gap-1.5 text-xs"
                        >
                          <RefreshCw className={`h-3.5 w-3.5 ${isGenerating ? "animate-spin" : ""}`} />
                          Regenerate
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleDeleteSession(currentSession.id)}
                          className="text-xs text-red-600 hover:text-red-700 hover:bg-red-50"
                        >
                          Delete
                        </Button>
                      </div>
                    </div>
                  </CardHeader>

                  <CardContent className="p-5">
                    {/* KEYWORD & SCORE DIAGNOSTIC */}
                    {currentSession.keywordCoverage && (
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                        <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3.5 flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-100 text-emerald-700 font-extrabold text-base">
                            {currentSession.keywordCoverage.matchScoreBefore}%
                          </div>
                          <div>
                            <p className="text-xs font-medium text-muted-foreground">Original Alignment</p>
                            <p className="text-sm font-semibold text-slate-900">Baseline Keyword Fit</p>
                          </div>
                        </div>

                        <div className="rounded-xl border border-amber-200 bg-amber-50/40 p-3.5 flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-amber-100 text-amber-800 font-extrabold text-base">
                            {currentSession.keywordCoverage.matchScoreAfter}%
                          </div>
                          <div>
                            <p className="text-xs font-medium text-amber-700">Projected Tailored Fit</p>
                            <p className="text-sm font-semibold text-amber-950">+{currentSession.keywordCoverage.matchScoreAfter - currentSession.keywordCoverage.matchScoreBefore}% Optimization</p>
                          </div>
                        </div>

                        <div className="rounded-xl border border-indigo-200 bg-indigo-50/40 p-3.5 flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-100 text-indigo-700 font-bold">
                            <ShieldCheck className="h-5 w-5" />
                          </div>
                          <div>
                            <p className="text-xs font-medium text-indigo-700">Factual Integrity</p>
                            <p className="text-xs text-indigo-950 leading-tight">100% Truth Preservation Active</p>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* KEYWORDS PILLS */}
                    {currentSession.keywordCoverage && (
                      <div className="space-y-2 pt-2 border-t text-xs">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-semibold text-slate-700 shrink-0">Matched Keywords:</span>
                          {currentSession.keywordCoverage.matchedKeywords.slice(0, 8).map((k) => (
                            <Badge key={k} variant="success" className="text-[11px] font-normal">
                              ✓ {k}
                            </Badge>
                          ))}
                        </div>

                        {currentSession.keywordCoverage.missingQualifications.length > 0 && (
                          <div className="flex flex-wrap items-center gap-2 pt-1">
                            <span className="font-semibold text-amber-700 shrink-0">Missing Qualifications (Not In Resume):</span>
                            {currentSession.keywordCoverage.missingQualifications.slice(0, 5).map((q) => (
                              <Badge key={q} variant="warning" className="text-[11px] font-normal">
                                ⚠ {q}
                              </Badge>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </CardContent>
                </Card>

                {/* WORKSPACE NAVIGATION TABS */}
                <div className="flex items-center justify-between border-b border-slate-200 pb-3">
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setActiveTab("suggestions")}
                      className={`px-3.5 py-1.5 rounded-lg text-sm font-semibold transition-all ${
                        activeTab === "suggestions"
                          ? "bg-slate-900 text-white shadow-sm"
                          : "text-slate-600 hover:bg-slate-100"
                      }`}
                    >
                      Proposed Edits ({currentSession.suggestions.length})
                    </button>
                    <button
                      onClick={() => setActiveTab("preview")}
                      className={`px-3.5 py-1.5 rounded-lg text-sm font-semibold transition-all ${
                        activeTab === "preview"
                          ? "bg-slate-900 text-white shadow-sm"
                          : "text-slate-600 hover:bg-slate-100"
                      }`}
                    >
                      Tailored Preview & Versions ({currentSession.versions?.length || 0})
                    </button>
                  </div>

                  {activeTab === "suggestions" && (
                    <div className="flex items-center gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={handleBatchAcceptSafe}
                        className="text-xs gap-1"
                      >
                        <Check className="h-3.5 w-3.5 text-emerald-600" />
                        Accept Safe ({pendingCount})
                      </Button>
                      <Button
                        size="sm"
                        onClick={handleSaveVersion}
                        disabled={isSavingVersion}
                        className="text-xs bg-slate-900 hover:bg-slate-800 text-white gap-1.5"
                      >
                        <Sparkles className="h-3.5 w-3.5 text-amber-400" />
                        Save As Version
                      </Button>
                    </div>
                  )}
                </div>

                {/* TAB 1: SUGGESTIONS WORKSPACE */}
                {activeTab === "suggestions" && (
                  <div className="space-y-4">
                    {/* SECTION FILTERS */}
                    <div className="flex flex-wrap items-center gap-2 text-xs">
                      <span className="font-semibold text-slate-500 mr-1">Section:</span>
                      {(["ALL", "SUMMARY", "SKILLS", "EXPERIENCE", "PROJECTS", "FORMATTING"] as const).map((sec) => (
                        <button
                          key={sec}
                          onClick={() => setFilterSection(sec)}
                          className={`px-2.5 py-1 rounded-md transition-colors ${
                            filterSection === sec
                              ? "bg-primary text-white font-semibold"
                              : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                          }`}
                        >
                          {sec === "ALL" ? "All Sections" : sec}
                        </button>
                      ))}

                      <div className="ml-auto flex items-center gap-3 text-slate-600">
                        <span><strong className="text-emerald-700">{acceptedCount}</strong> Accepted</span>
                        <span><strong className="text-amber-700">{pendingCount}</strong> Pending</span>
                        <span><strong className="text-slate-500">{rejectedCount}</strong> Rejected</span>
                      </div>
                    </div>

                    {/* SUGGESTIONS LIST */}
                    {filteredSuggestions.length === 0 ? (
                      <Card className="p-8 text-center border-dashed">
                        <p className="text-sm text-muted-foreground">No suggestions found for this section.</p>
                      </Card>
                    ) : (
                      filteredSuggestions.map((s) => {
                        const isEditing = editingSuggestionId === s.id;
                        return (
                          <Card
                            key={s.id}
                            className={`border transition-all ${
                              s.status === "ACCEPTED"
                                ? "border-emerald-200 bg-emerald-50/20"
                                : s.status === "REJECTED"
                                ? "border-slate-200 bg-slate-50/50 opacity-60"
                                : s.status === "EDITED"
                                ? "border-indigo-200 bg-indigo-50/20"
                                : "border-slate-200 bg-white"
                            }`}
                          >
                            <CardHeader className="p-4 pb-2">
                              <div className="flex flex-wrap items-center justify-between gap-2">
                                <div className="flex items-center gap-2">
                                  <Badge variant="outline" className="text-[10px] font-bold tracking-wider">
                                    {s.section}
                                  </Badge>
                                  <h3 className="text-sm font-bold text-slate-900">{s.title}</h3>
                                </div>

                                <div className="flex items-center gap-2">
                                  <Badge
                                    variant={
                                      s.status === "ACCEPTED"
                                        ? "success"
                                        : s.status === "EDITED"
                                        ? "info"
                                        : s.status === "REJECTED"
                                        ? "secondary"
                                        : "warning"
                                    }
                                    className="text-[10px] uppercase font-bold"
                                  >
                                    {s.status}
                                  </Badge>
                                </div>
                              </div>
                            </CardHeader>

                            <CardContent className="p-4 pt-1 space-y-3">
                              {/* CONFIRMATION WARNING BANNER */}
                              {s.requiresUserConfirmation && (
                                <div className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-amber-900 text-xs flex items-start gap-2.5">
                                  <AlertTriangle className="h-4 w-4 text-amber-600 mt-0.5 shrink-0" />
                                  <div className="space-y-1">
                                    <p className="font-bold">Factual Confirmation Required</p>
                                    <p className="text-amber-800">
                                      {s.confirmationPrompt ||
                                        "Only accept if you possess practical experience with this tool. Do NOT accept if you do not have practical competency."}
                                    </p>
                                  </div>
                                </div>
                              )}

                              {/* ORIGINAL VS PROPOSED DIFF */}
                              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                                <div className="rounded-lg border border-slate-200 bg-slate-50/80 p-3 space-y-1">
                                  <p className="font-semibold text-slate-500 uppercase text-[10px]">Original Text</p>
                                  <p className="text-slate-700 leading-relaxed font-mono whitespace-pre-wrap">
                                    {s.originalContent || "No existing bullet specified."}
                                  </p>
                                </div>

                                <div className="rounded-lg border border-indigo-200 bg-indigo-50/40 p-3 space-y-1">
                                  <p className="font-semibold text-indigo-700 uppercase text-[10px]">
                                    {s.status === "EDITED" ? "User Customized Text" : "Proposed ATS Optimization"}
                                  </p>

                                  {isEditing ? (
                                    <div className="space-y-2">
                                      <textarea
                                        value={editedText}
                                        onChange={(e) => setEditedText(e.target.value)}
                                        rows={4}
                                        className="w-full text-xs font-mono p-2 border rounded-md border-indigo-300 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                                      />
                                      <div className="flex justify-end gap-2">
                                        <Button
                                          size="sm"
                                          variant="outline"
                                          onClick={() => setEditingSuggestionId(null)}
                                          className="text-xs h-7 px-2"
                                        >
                                          Cancel
                                        </Button>
                                        <Button
                                          size="sm"
                                          onClick={() =>
                                            handleSuggestionDecision(s.id, "EDITED", editedText)
                                          }
                                          className="text-xs h-7 px-2 bg-indigo-600 hover:bg-indigo-700 text-white"
                                        >
                                          Save Edit
                                        </Button>
                                      </div>
                                    </div>
                                  ) : (
                                    <p className="text-slate-900 leading-relaxed font-mono whitespace-pre-wrap font-medium">
                                      {s.status === "EDITED" && s.userEditedContent
                                        ? s.userEditedContent
                                        : s.suggestedContent}
                                    </p>
                                  )}
                                </div>
                              </div>

                              {/* EXPLANATION */}
                              <div className="text-xs text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-100 flex items-start gap-2">
                                <Info className="h-4 w-4 text-slate-400 mt-0.5 shrink-0" />
                                <p className="leading-relaxed">{s.explanation}</p>
                              </div>

                              {/* ACTION BUTTONS */}
                              {!isEditing && (
                                <div className="flex items-center justify-end gap-2 pt-1 border-t">
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    onClick={() => {
                                      setEditingSuggestionId(s.id);
                                      setEditedText(s.userEditedContent || s.suggestedContent);
                                    }}
                                    className="text-xs h-8 gap-1"
                                  >
                                    <Edit3 className="h-3.5 w-3.5 text-slate-600" />
                                    Edit
                                  </Button>
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    onClick={() => handleSuggestionDecision(s.id, "REJECTED")}
                                    className={`text-xs h-8 gap-1 ${
                                      s.status === "REJECTED" ? "bg-red-50 border-red-200 text-red-700" : ""
                                    }`}
                                  >
                                    <XCircle className="h-3.5 w-3.5 text-red-600" />
                                    Reject
                                  </Button>
                                  <Button
                                    size="sm"
                                    onClick={() => handleSuggestionDecision(s.id, "ACCEPTED")}
                                    className={`text-xs h-8 gap-1 ${
                                      s.status === "ACCEPTED"
                                        ? "bg-emerald-600 text-white"
                                        : "bg-emerald-50 text-emerald-800 hover:bg-emerald-100 border border-emerald-200"
                                    }`}
                                  >
                                    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                                    Accept
                                  </Button>
                                </div>
                              )}
                            </CardContent>
                          </Card>
                        );
                      })
                    )}
                  </div>
                )}

                {/* TAB 2: PREVIEW & VERSION HISTORY */}
                {activeTab === "preview" && (
                  <div className="space-y-6">
                    {/* VERSIONS SELECTOR */}
                    <div className="flex flex-wrap items-center justify-between gap-3 bg-slate-50 p-4 rounded-xl border">
                      <div className="flex items-center gap-2">
                        <History className="h-4 w-4 text-slate-500" />
                        <span className="text-xs font-bold uppercase tracking-wider text-slate-700">Saved Versions:</span>
                        {currentSession.versions && currentSession.versions.length > 0 ? (
                          <div className="flex flex-wrap items-center gap-2">
                            {currentSession.versions.map((v) => (
                              <button
                                key={v.id}
                                onClick={() => setActiveVersion(v)}
                                className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                                  activeVersion?.id === v.id
                                    ? "bg-slate-900 text-white shadow-sm"
                                    : "bg-white border text-slate-700 hover:bg-slate-100"
                                }`}
                              >
                                {v.versionName}
                              </button>
                            ))}
                          </div>
                        ) : (
                          <span className="text-xs text-muted-foreground">No versions saved yet. Click "Save As Version" above.</span>
                        )}
                      </div>

                      {activeVersion && (
                        <div className="flex items-center gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={handleCopyText}
                            className="text-xs h-8 gap-1"
                          >
                            <Copy className="h-3.5 w-3.5" />
                            {copiedText ? "Copied!" : "Copy Text"}
                          </Button>

                          <Button
                            size="sm"
                            onClick={handleDownloadPdf}
                            disabled={isExportingPdf}
                            className="text-xs h-8 bg-primary hover:bg-primary/90 text-white gap-1.5"
                          >
                            <Download className={`h-3.5 w-3.5 ${isExportingPdf ? "animate-spin" : ""}`} />
                            {isExportingPdf ? "Generating PDF..." : "Download PDF"}
                          </Button>
                        </div>
                      )}
                    </div>

                    {/* RESUME PREVIEW CONTAINER */}
                    {activeVersion ? (
                      <Card className="border-slate-300 shadow-sm bg-white font-sans max-w-4xl mx-auto p-8 sm:p-12 space-y-6 border rounded-xl">
                        {/* Resume Header */}
                        <div className="border-b pb-4 text-center sm:text-left space-y-1">
                          <h2 className="text-2xl font-extrabold text-slate-900 uppercase tracking-tight">
                            {activeVersion.tailoredContent?.candidateName || "Candidate Resume"}
                          </h2>
                          <p className="text-sm font-bold text-indigo-700">
                            {currentSession.targetRole || "Software Engineer"}
                          </p>
                          <p className="text-xs text-slate-500">
                            {activeVersion.tailoredContent?.email || "candidate@example.com"} |{" "}
                            {activeVersion.tailoredContent?.phone || "(555) 019-2831"} |{" "}
                            {activeVersion.tailoredContent?.location || "Remote"}
                          </p>
                        </div>

                        {/* Professional Summary */}
                        {activeVersion.tailoredContent?.professionalSummary && (
                          <div className="space-y-1.5">
                            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-900 border-b pb-1">
                              Professional Summary
                            </h3>
                            <p className="text-xs text-slate-700 leading-relaxed">
                              {activeVersion.tailoredContent.professionalSummary}
                            </p>
                          </div>
                        )}

                        {/* Core Competencies */}
                        {activeVersion.tailoredContent?.skills && (
                          <div className="space-y-1.5">
                            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-900 border-b pb-1">
                              Core Competencies & Technical Skills
                            </h3>
                            <p className="text-xs text-slate-700 leading-relaxed">
                              {activeVersion.tailoredContent.skills.join("  •  ")}
                            </p>
                          </div>
                        )}

                        {/* Professional Experience */}
                        {activeVersion.tailoredContent?.experience && (
                          <div className="space-y-3">
                            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-900 border-b pb-1">
                              Professional Experience
                            </h3>
                            {activeVersion.tailoredContent.experience.map((exp, idx) => (
                              <div key={idx} className="space-y-1">
                                <div className="flex justify-between items-baseline text-xs font-semibold text-slate-900">
                                  <span>{exp.roleTitle} — {exp.company}</span>
                                  <span className="text-slate-500 font-normal">{exp.duration}</span>
                                </div>
                                <ul className="list-disc list-inside text-xs text-slate-700 space-y-1 pl-1">
                                  {exp.bullets.map((b, bIdx) => (
                                    <li key={bIdx} className="leading-relaxed">
                                      {b}
                                    </li>
                                  ))}
                                </ul>
                              </div>
                            ))}
                          </div>
                        )}

                        {/* Projects */}
                        {activeVersion.tailoredContent?.projects && activeVersion.tailoredContent.projects.length > 0 && (
                          <div className="space-y-3">
                            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-900 border-b pb-1">
                              Technical Projects
                            </h3>
                            {activeVersion.tailoredContent.projects.map((prj, idx) => (
                              <div key={idx} className="space-y-1">
                                <div className="flex justify-between items-baseline text-xs font-semibold text-slate-900">
                                  <span>{prj.title}</span>
                                  <span className="text-indigo-600 text-[11px] font-mono">
                                    {prj.techStack?.join(", ")}
                                  </span>
                                </div>
                                {prj.description && (
                                  <p className="text-xs text-slate-600">{prj.description}</p>
                                )}
                                <ul className="list-disc list-inside text-xs text-slate-700 space-y-1 pl-1">
                                  {prj.bullets?.map((b, bIdx) => (
                                    <li key={bIdx} className="leading-relaxed">
                                      {b}
                                    </li>
                                  ))}
                                </ul>
                              </div>
                            ))}
                          </div>
                        )}

                        {/* Education */}
                        {activeVersion.tailoredContent?.education && (
                          <div className="space-y-1.5">
                            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-900 border-b pb-1">
                              Education & Credentials
                            </h3>
                            <ul className="list-disc list-inside text-xs text-slate-700 space-y-1 pl-1">
                              {activeVersion.tailoredContent.education.map((edu, idx) => (
                                <li key={idx}>{edu}</li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </Card>
                    ) : (
                      <Card className="p-12 text-center border-dashed">
                        <CardContent className="space-y-3">
                          <p className="text-sm font-semibold text-slate-800">
                            No version generated yet.
                          </p>
                          <p className="text-xs text-muted-foreground max-w-md mx-auto">
                            Review and accept suggestions in the "Proposed Edits" tab, then click "Save As Version" to build and export your tailored resume.
                          </p>
                          <Button size="sm" onClick={() => setActiveTab("suggestions")}>
                            Review Suggestions Now
                          </Button>
                        </CardContent>
                      </Card>
                    )}
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      )}

      {/* 3. NEW SESSION MODAL */}
      {showNewSessionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
          <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <Wand2 className="h-5 w-5 text-amber-600" />
                <h3 className="text-lg font-bold text-slate-900">Create Resume Tailoring Session</h3>
              </div>
              <button
                onClick={() => setShowNewSessionModal(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateSession} className="space-y-4 text-xs">
              {/* Source Resume Selection */}
              <div className="space-y-1.5">
                <label className="font-semibold text-slate-700">1. Select Source Resume</label>
                {resumes.length === 0 ? (
                  <p className="text-red-600 text-xs">
                    No uploaded resumes found. Please upload a resume first via Resume Analyzer.
                  </p>
                ) : (
                  <select
                    value={selectedResumeId}
                    onChange={(e) => setSelectedResumeId(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 p-2.5 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-primary"
                  >
                    {resumes.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.originalFilename} (Uploaded {new Date(r.createdAt).toLocaleDateString()})
                      </option>
                    ))}
                  </select>
                )}
              </div>

              {/* Target Job Toggle */}
              <div className="space-y-1.5">
                <label className="font-semibold text-slate-700">2. Target Job Source</label>
                <div className="flex rounded-lg border p-1 bg-slate-50 gap-1">
                  <button
                    type="button"
                    onClick={() => setJobMode("stored")}
                    className={`flex-1 py-1.5 rounded-md font-semibold text-xs transition-all ${
                      jobMode === "stored" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"
                    }`}
                  >
                    From Saved / Catalog Jobs
                  </button>
                  <button
                    type="button"
                    onClick={() => setJobMode("manual")}
                    className={`flex-1 py-1.5 rounded-md font-semibold text-xs transition-all ${
                      jobMode === "manual" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"
                    }`}
                  >
                    Enter Manually
                  </button>
                </div>
              </div>

              {jobMode === "stored" ? (
                <div className="space-y-1.5">
                  <label className="font-semibold text-slate-700">Select Job Posting</label>
                  {availableJobs.length === 0 ? (
                    <p className="text-slate-500 text-xs">No catalog jobs found. Please use the manual option.</p>
                  ) : (
                    <select
                      value={selectedJobId}
                      onChange={(e) => setSelectedJobId(e.target.value)}
                      className="w-full rounded-lg border border-slate-300 p-2.5 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-primary"
                    >
                      {availableJobs.map((j) => (
                        <option key={j.id} value={j.id}>
                          {j.title} — {j.company} ({j.location})
                        </option>
                      ))}
                    </select>
                  )}
                </div>
              ) : (
                <div className="space-y-3">
                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-1">
                      <label className="font-semibold text-slate-700">Target Role Title</label>
                      <input
                        type="text"
                        value={customJobTitle}
                        onChange={(e) => setCustomJobTitle(e.target.value)}
                        placeholder="e.g. Senior Java Developer"
                        className="w-full rounded-lg border border-slate-300 p-2 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-primary"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="font-semibold text-slate-700">Company Name</label>
                      <input
                        type="text"
                        value={customCompanyName}
                        onChange={(e) => setCustomCompanyName(e.target.value)}
                        placeholder="e.g. Stripe, Acme Corp"
                        className="w-full rounded-lg border border-slate-300 p-2 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-primary"
                      />
                    </div>
                  </div>

                  <div className="space-y-1">
                    <label className="font-semibold text-slate-700">Job Description & Requirements</label>
                    <textarea
                      value={customJobDescription}
                      onChange={(e) => setCustomJobDescription(e.target.value)}
                      rows={5}
                      placeholder="Paste the job requirements, responsibilities, and tech stack here..."
                      className="w-full rounded-lg border border-slate-300 p-2.5 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-primary font-mono"
                    />
                  </div>
                </div>
              )}

              {/* Factual Accuracy Reminder */}
              <div className="rounded-lg bg-amber-50/70 border border-amber-200 p-3 text-[11px] text-amber-900 flex items-start gap-2">
                <ShieldCheck className="h-4 w-4 text-amber-600 mt-0.5 shrink-0" />
                <span>
                  <strong>Strict Accuracy Policy:</strong> The AI will optimize phrasing and keywords, but will never invent employers, degrees, metrics, or skills you haven't confirmed.
                </span>
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setShowNewSessionModal(false)}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  size="sm"
                  disabled={isCreating}
                  className="bg-primary hover:bg-primary/90 text-white font-semibold gap-1.5"
                >
                  {isCreating ? (
                    <>
                      <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                      Initializing...
                    </>
                  ) : (
                    <>
                      <Sparkles className="h-3.5 w-3.5 text-amber-400" />
                      Create & Analyze
                    </>
                  )}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 4. FOOTER DISCLAIMER */}
      <div className="rounded-xl border border-slate-200/80 bg-slate-50 p-4 text-xs text-slate-500 flex items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <ShieldCheck className="h-4 w-4 text-emerald-600 shrink-0" />
          <span>
            <strong>Factual Accuracy Guarantee:</strong> CareerPilot strictly forbids hallucinations. Tailoring scores indicate ATS keyword density and alignment, not guarantees of employment.
          </span>
        </div>
        <Link to="/resume-analyzer" className="text-primary hover:underline font-semibold shrink-0">
          Source Resumes →
        </Link>
      </div>
    </div>
  );
};
