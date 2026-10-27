import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "@/context/AuthContext";
import {
  FileText,
  UploadCloud,
  CheckCircle2,
  AlertTriangle,
  AlertCircle,
  Trash2,
  RefreshCw,
  Briefcase,
  Award,
  TrendingUp,
  Sparkles,
  Info,
  ShieldCheck,
  Layers,
} from "lucide-react";
import {
  ResumeSummary,
  ResumeAnalysis,
  uploadResume,
  fetchUserResumes,
  deleteResume,
  triggerResumeAnalysis,
  fetchResumeAnalysisById,
  fetchJobs,
  fetchSavedJobs,
  JobSummaryResponse,
} from "@/lib/api";

export const ResumePage: React.FC = () => {
  const { session } = useAuth();
  const token = session?.access_token;

  const [resumes, setResumes] = useState<ResumeSummary[]>([]);
  const [selectedResumeId, setSelectedResumeId] = useState<string | null>(null);
  const [currentAnalysis, setCurrentAnalysis] = useState<ResumeAnalysis | null>(null);

  const [availableJobs, setAvailableJobs] = useState<JobSummaryResponse[]>([]);
  const [selectedJobId, setSelectedJobId] = useState<string>("");

  const [isLoadingList, setIsLoadingList] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [analysisError, setAnalysisError] = useState<string | null>(null);

  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Load user resumes and available jobs on mount
  useEffect(() => {
    if (token) {
      loadResumes();
      loadJobs();
    }
  }, [token]);

  const loadResumes = async () => {
    if (!token) return;
    setIsLoadingList(true);
    try {
      const data = await fetchUserResumes(token);
      setResumes(data);
      if (data.length > 0 && !selectedResumeId) {
        setSelectedResumeId(data[0].id);
        if (data[0].latestAnalysisId) {
          loadAnalysis(data[0].id, data[0].latestAnalysisId);
        }
      }
    } catch (err: any) {
      console.error("Failed to load resumes:", err);
    } finally {
      setIsLoadingList(false);
    }
  };

  const loadJobs = async () => {
    if (!token) return;
    try {
      const [allJobsRes, savedJobsRes] = await Promise.all([
        fetchJobs({ page: 0, size: 20 }, token),
        fetchSavedJobs(token).catch(() => null),
      ]);
      const mergedMap = new Map<string, JobSummaryResponse>();
      if (savedJobsRes?.content) {
        savedJobsRes.content.forEach((j: JobSummaryResponse) => mergedMap.set(j.id, j));
      }
      if (allJobsRes?.content) {
        allJobsRes.content.forEach((j: JobSummaryResponse) => {
          if (!mergedMap.has(j.id)) mergedMap.set(j.id, j);
        });
      }
      setAvailableJobs(Array.from(mergedMap.values()));
    } catch (err) {
      console.warn("Failed to load catalog jobs:", err);
    }
  };

  const loadAnalysis = async (resumeId: string, analysisId: string) => {
    if (!token) return;
    setIsAnalyzing(true);
    setAnalysisError(null);
    try {
      const analysis = await fetchResumeAnalysisById(resumeId, analysisId, token);
      setCurrentAnalysis(analysis);
    } catch (err: any) {
      setAnalysisError(err.message || "Failed to load previous analysis.");
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleFileUpload = async (file: File) => {
    if (!token) return;
    setUploadError(null);

    if (!file.name.toLowerCase().endsWith(".pdf")) {
      setUploadError("Only PDF documents (.pdf) are supported at this time.");
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      setUploadError("File exceeds the 10MB limit. Please upload a smaller document.");
      return;
    }

    setIsUploading(true);
    try {
      const newResume = await uploadResume(file, token);
      setResumes((prev) => [newResume, ...prev]);
      setSelectedResumeId(newResume.id);
      setCurrentAnalysis(null);
      // Auto-trigger initial baseline analysis
      runAnalysis(newResume.id);
    } catch (err: any) {
      setUploadError(err.message || "Failed to upload resume.");
    } finally {
      setIsUploading(false);
    }
  };

  const runAnalysis = async (resumeId: string, jobId?: string) => {
    if (!token) return;
    setIsAnalyzing(true);
    setAnalysisError(null);
    try {
      const analysis = await triggerResumeAnalysis(resumeId, token, jobId || undefined);
      setCurrentAnalysis(analysis);
      // Update local resume score
      setResumes((prev) =>
        prev.map((r) =>
          r.id === resumeId
            ? { ...r, latestScore: analysis.overallScore, latestAnalysisId: analysis.id }
            : r
        )
      );
    } catch (err: any) {
      setAnalysisError(err.message || "Resume analysis encountered an error.");
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleDeleteResume = async (resumeId: string) => {
    if (!token) return;
    if (!confirm("Are you sure you want to delete this resume and its analysis history?")) return;

    try {
      await deleteResume(resumeId, token);
      const remaining = resumes.filter((r) => r.id !== resumeId);
      setResumes(remaining);
      if (selectedResumeId === resumeId) {
        if (remaining.length > 0) {
          setSelectedResumeId(remaining[0].id);
          if (remaining[0].latestAnalysisId) {
            loadAnalysis(remaining[0].id, remaining[0].latestAnalysisId);
          } else {
            setCurrentAnalysis(null);
          }
        } else {
          setSelectedResumeId(null);
          setCurrentAnalysis(null);
        }
      }
    } catch (err: any) {
      alert(err.message || "Failed to delete resume.");
    }
  };

  const getScoreColor = (score: number) => {
    if (score >= 80) return "text-emerald-700 bg-emerald-50 border-emerald-200";
    if (score >= 65) return "text-indigo-700 bg-indigo-50 border-indigo-200";
    if (score >= 50) return "text-amber-700 bg-amber-50 border-amber-200";
    return "text-rose-700 bg-rose-50 border-rose-200";
  };

  const getScoreBadgeText = (score: number) => {
    if (score >= 80) return "Exceptional ATS Readiness";
    if (score >= 65) return "Competitive Industry Profile";
    if (score >= 50) return "Moderate — Revisions Recommended";
    return "Critical Gaps Detected";
  };

  const getPriorityBadge = (priority: string) => {
    switch (priority.toUpperCase()) {
      case "HIGH":
        return "bg-rose-100 text-rose-800 border-rose-200";
      case "MEDIUM":
        return "bg-amber-100 text-amber-800 border-amber-200";
      default:
        return "bg-blue-100 text-blue-800 border-blue-200";
    }
  };

  const selectedResume = resumes.find((r) => r.id === selectedResumeId);

  return (
    <div className="space-y-8 max-w-7xl mx-auto pb-12">
      {/* Header Section */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-6">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              AI Resume Analyzer
            </h1>
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-emerald-100 text-emerald-800 border border-emerald-200">
              Phase 4 Ready
            </span>
          </div>
          <p className="text-sm text-slate-600">
            Upload your resume in PDF format to receive instant rubric-based scoring, ATS parsing audits, and targeted bullet recommendations.
          </p>
        </div>
      </div>

      {/* Upload Zone & Saved Resumes Bar */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Upload Drop Zone */}
        <div className="lg:col-span-2">
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setIsDragging(true);
            }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={(e) => {
              e.preventDefault();
              setIsDragging(false);
              if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                handleFileUpload(e.dataTransfer.files[0]);
              }
            }}
            onClick={() => fileInputRef.current?.click()}
            className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all duration-150 ${
              isDragging
                ? "border-indigo-600 bg-indigo-50/50"
                : "border-slate-300 hover:border-indigo-400 bg-white"
            } ${isUploading ? "opacity-60 pointer-events-none" : ""}`}
          >
            <input
              type="file"
              ref={fileInputRef}
              accept="application/pdf"
              className="hidden"
              onChange={(e) => {
                if (e.target.files && e.target.files[0]) {
                  handleFileUpload(e.target.files[0]);
                }
              }}
            />
            <div className="mx-auto w-12 h-12 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-600 mb-3">
              {isUploading ? (
                <RefreshCw className="h-6 w-6 animate-spin text-indigo-600" />
              ) : (
                <UploadCloud className="h-6 w-6" />
              )}
            </div>
            <h3 className="text-sm font-semibold text-slate-900">
              {isUploading ? "Processing Document..." : "Click or drag & drop your resume PDF"}
            </h3>
            <p className="text-xs text-slate-500 mt-1">
              Supports genuine PDF text files up to 10MB.
            </p>
            <div className="flex items-center justify-center gap-3 mt-3 text-xs text-slate-400">
              <span className="flex items-center gap-1">
                <ShieldCheck className="h-3.5 w-3.5 text-emerald-600" /> Private & Secure
              </span>
              <span>•</span>
              <span>Never shared with external third parties</span>
            </div>
          </div>

          {uploadError && (
            <div className="mt-3 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center gap-2">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{uploadError}</span>
            </div>
          )}
        </div>

        {/* Existing Resumes Selector */}
        <div className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3 pb-2 border-b border-slate-100">
              <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                <FileText className="h-4 w-4 text-slate-400" /> Uploaded Resumes ({resumes.length})
              </h2>
            </div>

            {isLoadingList ? (
              <div className="py-8 text-center text-xs text-slate-400">Loading documents...</div>
            ) : resumes.length === 0 ? (
              <div className="py-6 text-center text-xs text-slate-400">
                No resumes uploaded yet. Upload a PDF above to begin.
              </div>
            ) : (
              <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                {resumes.map((r) => (
                  <div
                    key={r.id}
                    onClick={() => {
                      setSelectedResumeId(r.id);
                      if (r.latestAnalysisId) {
                        loadAnalysis(r.id, r.latestAnalysisId);
                      } else {
                        setCurrentAnalysis(null);
                      }
                    }}
                    className={`p-2.5 rounded-lg border text-left cursor-pointer transition flex items-center justify-between ${
                      selectedResumeId === r.id
                        ? "border-indigo-600 bg-indigo-50/40 text-indigo-950 font-medium"
                        : "border-slate-200 hover:border-slate-300 text-slate-700 bg-slate-50/50"
                    }`}
                  >
                    <div className="truncate mr-2">
                      <div className="text-xs truncate font-medium">{r.originalFilename}</div>
                      <div className="text-[11px] text-slate-400">
                        {(r.fileSize / 1024).toFixed(0)} KB • {new Date(r.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0">
                      {r.latestScore !== null && (
                        <span className={`px-1.5 py-0.5 rounded text-[11px] font-semibold border ${getScoreColor(r.latestScore)}`}>
                          {r.latestScore}/100
                        </span>
                      )}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteResume(r.id);
                        }}
                        className="text-slate-400 hover:text-rose-600 p-1 rounded"
                        title="Delete resume"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {selectedResume && (
            <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
              <span className="text-slate-500">Selected for analysis</span>
              <button
                onClick={() => runAnalysis(selectedResume.id, selectedJobId || undefined)}
                disabled={isAnalyzing}
                className="inline-flex items-center gap-1 text-indigo-600 hover:text-indigo-700 font-semibold"
              >
                <RefreshCw className={`h-3 w-3 ${isAnalyzing ? "animate-spin" : ""}`} />
                {isAnalyzing ? "Analyzing..." : "Re-Analyze"}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Job-Specific Targeted Matching Panel */}
      {selectedResume && (
        <div className="bg-gradient-to-r from-slate-900 to-indigo-950 text-white rounded-xl p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <Sparkles className="h-4 w-4 text-indigo-400" />
                <h3 className="text-sm font-semibold text-white">Target Job Compatibility Audit</h3>
              </div>
              <p className="text-xs text-slate-300">
                Compare this resume against an active requisition to detect missing requirements and receive tailored bullet recommendations.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <select
                value={selectedJobId}
                onChange={(e) => setSelectedJobId(e.target.value)}
                className="bg-slate-800 text-xs text-white border border-slate-700 rounded-lg px-3 py-2 outline-none focus:border-indigo-400 max-w-xs truncate"
              >
                <option value="">General Review (No Target Role)</option>
                {availableJobs.map((job) => (
                  <option key={job.id} value={job.id}>
                    {job.title} — {job.company}
                  </option>
                ))}
              </select>

              <button
                onClick={() => runAnalysis(selectedResume.id, selectedJobId || undefined)}
                disabled={isAnalyzing}
                className="px-3 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shrink-0 transition disabled:opacity-50"
              >
                {isAnalyzing ? "Evaluating..." : "Run Job Audit"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Analysis Error Notification */}
      {analysisError && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-sm flex items-center gap-2">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span>{analysisError}</span>
        </div>
      )}

      {/* Analyzing Progress State */}
      {isAnalyzing && (
        <div className="bg-white border border-slate-200 rounded-xl p-12 text-center">
          <div className="w-12 h-12 rounded-full border-4 border-indigo-200 border-t-indigo-600 animate-spin mx-auto mb-4" />
          <h3 className="text-base font-semibold text-slate-900">Evaluating Resume Content</h3>
          <p className="text-xs text-slate-500 max-w-md mx-auto mt-1">
            Running 4-dimension rubric scoring, verifying project evidence, checking measurable impact metrics, and generating tailored improvement actions...
          </p>
        </div>
      )}

      {/* Analysis Results View */}
      {!isAnalyzing && currentAnalysis && (
        <div className="space-y-6">
          {/* Top Score & Dimension Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Overall Score Card */}
            <div className="bg-white border border-slate-200 rounded-xl p-6 flex flex-col justify-between shadow-sm">
              <div>
                <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">
                  Diagnostic Score
                </div>
                <div className="flex items-baseline gap-2">
                  <span className="text-5xl font-black tracking-tight text-slate-900">
                    {currentAnalysis.overallScore}
                  </span>
                  <span className="text-slate-400 font-semibold text-lg">/ 100</span>
                </div>
                <div className={`mt-3 inline-block px-2.5 py-1 rounded-full text-xs font-semibold border ${getScoreColor(currentAnalysis.overallScore)}`}>
                  {getScoreBadgeText(currentAnalysis.overallScore)}
                </div>
              </div>

              <div className="mt-4 pt-3 border-t border-slate-100 text-[11px] text-slate-500 flex items-start gap-1.5">
                <Info className="h-3.5 w-3.5 shrink-0 text-slate-400 mt-0.5" />
                <span>
                  Evaluated using rubric v1: Skills (25%), Impact (30%), Structure (25%), Specificity (20%).
                </span>
              </div>
            </div>

            {/* Rubric Breakdown (3 cols) */}
            <div className="lg:col-span-3 bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
              <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4">
                Rubric Dimension Breakdown
              </h3>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {currentAnalysis.dimensionScores.map((dim, idx) => (
                  <div key={idx} className="p-3.5 rounded-lg border border-slate-100 bg-slate-50/60">
                    <div className="flex items-center justify-between text-xs font-semibold text-slate-900 mb-1.5">
                      <span>{dim.dimension}</span>
                      <span className="font-bold text-indigo-700">
                        {dim.score} / {dim.maxScore}
                      </span>
                    </div>
                    {/* Progress Bar */}
                    <div className="w-full h-2 rounded-full bg-slate-200 overflow-hidden mb-2">
                      <div
                        className="h-full bg-indigo-600 rounded-full transition-all duration-300"
                        style={{ width: `${(dim.score / dim.maxScore) * 100}%` }}
                      />
                    </div>
                    <p className="text-[11px] text-slate-600 leading-relaxed">
                      {dim.feedback}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Job Match Card (Shown if target job was audited) */}
          {currentAnalysis.jobMatch && (
            <div className="bg-white border border-indigo-200 rounded-xl p-6 shadow-sm">
              <div className="flex items-center justify-between border-b border-indigo-100 pb-3 mb-4">
                <div className="flex items-center gap-2">
                  <Briefcase className="h-5 w-5 text-indigo-600" />
                  <div>
                    <h3 className="text-sm font-bold text-slate-900">
                      Target Role Match: {currentAnalysis.jobMatch.jobTitle}
                    </h3>
                    <div className="text-xs text-slate-500">{currentAnalysis.jobMatch.company}</div>
                  </div>
                </div>
                {currentAnalysis.jobMatch.matchScore !== null && (
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs text-slate-500">Compatibility:</span>
                    <span className="px-2 py-0.5 rounded font-bold text-xs bg-indigo-100 text-indigo-800 border border-indigo-200">
                      {currentAnalysis.jobMatch.matchScore}%
                    </span>
                  </div>
                )}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <h4 className="text-xs font-semibold text-emerald-800 uppercase tracking-wider mb-2 flex items-center gap-1">
                    <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" /> Verified Requirements in Resume
                  </h4>
                  <div className="flex flex-wrap gap-1.5">
                    {currentAnalysis.jobMatch.matchedSkills.length > 0 ? (
                      currentAnalysis.jobMatch.matchedSkills.map((s, i) => (
                        <span key={i} className="px-2 py-1 rounded text-xs bg-emerald-50 text-emerald-800 border border-emerald-200 font-medium">
                          {s}
                        </span>
                      ))
                    ) : (
                      <span className="text-xs text-slate-400">No direct skill matches detected.</span>
                    )}
                  </div>
                </div>

                <div>
                  <h4 className="text-xs font-semibold text-amber-800 uppercase tracking-wider mb-2 flex items-center gap-1">
                    <AlertTriangle className="h-3.5 w-3.5 text-amber-600" /> Missing Required Skills
                  </h4>
                  <div className="flex flex-wrap gap-1.5">
                    {currentAnalysis.jobMatch.missingRequiredSkills.length > 0 ? (
                      currentAnalysis.jobMatch.missingRequiredSkills.map((s, i) => (
                        <span key={i} className="px-2 py-1 rounded text-xs bg-amber-50 text-amber-800 border border-amber-200 font-medium">
                          {s}
                        </span>
                      ))
                    ) : (
                      <span className="text-xs text-emerald-600 font-medium">
                        All mandatory technical requirements accounted for!
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {currentAnalysis.jobMatch.tailoredRecommendations.length > 0 && (
                <div className="mt-4 pt-3 border-t border-indigo-50 text-xs text-slate-700">
                  <span className="font-semibold text-indigo-900">Target Role Action: </span>
                  {currentAnalysis.jobMatch.tailoredRecommendations.join(" ")}
                </div>
              )}
            </div>
          )}

          {/* Executive Summary Card */}
          <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
            <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <Award className="h-4 w-4 text-indigo-600" /> Executive Profile Summary
            </h3>
            <p className="text-sm text-slate-700 leading-relaxed mb-4">
              {currentAnalysis.summary}
            </p>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-3 border-t border-slate-100 text-xs">
              <div>
                <span className="text-slate-400 block">Candidate</span>
                <span className="font-semibold text-slate-800 truncate block">
                  {currentAnalysis.extractedData.candidateName || "Candidate"}
                </span>
              </div>
              <div>
                <span className="text-slate-400 block">Email</span>
                <span className="font-semibold text-slate-800 truncate block">
                  {currentAnalysis.extractedData.email || "Not specified"}
                </span>
              </div>
              <div>
                <span className="text-slate-400 block">Word Count</span>
                <span className="font-semibold text-slate-800">
                  {currentAnalysis.extractedData.wordCount} words
                </span>
              </div>
              <div>
                <span className="text-slate-400 block">Quantified Metrics</span>
                <span className="font-semibold text-emerald-700">
                  {currentAnalysis.extractedData.quantifiableMetricsCount} detected
                </span>
              </div>
            </div>
          </div>

          {/* Actionable Improvements Section */}
          <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
            <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 flex items-center gap-1.5">
              <TrendingUp className="h-4 w-4 text-indigo-600" /> Prioritized Improvement Actions ({currentAnalysis.improvements.length})
            </h3>

            <div className="space-y-4">
              {currentAnalysis.improvements.map((imp, idx) => (
                <div key={idx} className="border border-slate-200 rounded-lg p-4 bg-slate-50/40">
                  <div className="flex items-center justify-between gap-2 mb-2">
                    <div className="flex items-center gap-2">
                      <span className={`px-2 py-0.5 rounded text-[11px] font-bold border uppercase ${getPriorityBadge(imp.priority)}`}>
                        {imp.priority} Priority
                      </span>
                      <span className="text-sm font-bold text-slate-900">{imp.title}</span>
                    </div>
                    <span className="text-xs font-medium text-slate-400">{imp.category}</span>
                  </div>

                  <p className="text-xs text-slate-700 mb-3">{imp.issue}</p>

                  <div className="p-3 bg-white rounded border border-indigo-100 space-y-2 text-xs">
                    <div className="font-semibold text-indigo-950 flex items-center gap-1">
                      <Sparkles className="h-3.5 w-3.5 text-indigo-600" /> Recommended Rewrite Strategy:
                    </div>
                    <p className="text-slate-600">{imp.suggestion}</p>

                    {imp.exampleBefore && imp.exampleAfter && (
                      <div className="mt-2 grid grid-cols-1 md:grid-cols-2 gap-2 pt-2 border-t border-slate-100 text-[11px]">
                        <div className="bg-rose-50/60 p-2 rounded border border-rose-100">
                          <span className="font-bold text-rose-800 block mb-0.5">Before (Weak):</span>
                          <span className="text-slate-600 italic">"{imp.exampleBefore}"</span>
                        </div>
                        <div className="bg-emerald-50/60 p-2 rounded border border-emerald-100">
                          <span className="font-bold text-emerald-800 block mb-0.5">After (High-Impact):</span>
                          <span className="text-slate-700 font-medium">"{imp.exampleAfter}"</span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Key Strengths & Validated Evidence */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Strengths */}
            <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
              <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 flex items-center gap-1.5">
                <CheckCircle2 className="h-4 w-4 text-emerald-600" /> Demonstrated Strengths
              </h3>

              <div className="space-y-3">
                {currentAnalysis.strengths.map((str, idx) => (
                  <div key={idx} className="p-3 rounded-lg border border-emerald-100 bg-emerald-50/30">
                    <h4 className="text-xs font-bold text-emerald-950 mb-1">{str.title}</h4>
                    <p className="text-xs text-slate-700 mb-2 leading-relaxed">{str.description}</p>
                    {str.evidence && (
                      <div className="text-[11px] text-slate-500 italic bg-white p-2 rounded border border-slate-100">
                        "{str.evidence}"
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Extracted Technical Skills with Context */}
            <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
              <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4 flex items-center gap-1.5">
                <Layers className="h-4 w-4 text-indigo-600" /> Extracted Skills & Project Evidence
              </h3>

              <div className="space-y-2 max-h-96 overflow-y-auto pr-1">
                {currentAnalysis.skillAnalysis.technicalSkills.map((ts, idx) => (
                  <div key={idx} className="p-2.5 rounded-lg border border-slate-100 bg-slate-50/50 flex flex-col gap-1 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-slate-900">{ts.name}</span>
                      <span className="text-[10px] text-slate-400">{ts.category}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      {ts.hasContext ? (
                        <span className="text-[10px] px-1.5 py-0.2 rounded font-medium bg-emerald-100 text-emerald-800">
                          Evidence Verified
                        </span>
                      ) : (
                        <span className="text-[10px] px-1.5 py-0.2 rounded font-medium bg-amber-100 text-amber-800">
                          Keyword Only
                        </span>
                      )}
                      <span className="text-[11px] text-slate-500 truncate">{ts.evidence}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
