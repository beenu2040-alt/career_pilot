import React, { useState, useEffect } from "react";
import {
  X,
  MapPin,
  Briefcase,
  Building,
  Bookmark,
  ExternalLink,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  Loader2,
  Sparkles,
  Send,
  Check,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  type JobDetailResponse,
  fetchJobDetail,
  saveJob,
  unsaveJob,
  createApplication,
} from "@/lib/api";
import { useAuth } from "@/context/AuthContext";

interface JobDetailModalProps {
  jobId: string | null;
  isOpen: boolean;
  onClose: () => void;
  onBookmarkToggled?: (jobId: string, isSaved: boolean) => void;
}

export const JobDetailModal: React.FC<JobDetailModalProps> = ({
  jobId,
  isOpen,
  onClose,
  onBookmarkToggled,
}) => {
  const { accessToken, user } = useAuth();
  const [job, setJob] = useState<JobDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isTracking, setIsTracking] = useState(false);
  const [trackStatus, setTrackStatus] = useState<"idle" | "tracked" | "exists">("idle");
  const [trackFeedback, setTrackFeedback] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen || !jobId) {
      setJob(null);
      setError(null);
      setTrackStatus("idle");
      setTrackFeedback(null);
      return;
    }

    let isMounted = true;
    const loadDetail = async () => {
      setIsLoading(true);
      setError(null);
      setTrackStatus("idle");
      setTrackFeedback(null);
      try {
        const data = await fetchJobDetail(jobId, accessToken);
        if (isMounted) {
          setJob(data);
        }
      } catch (err) {
        if (isMounted) {
          setError((err as Error).message || "Failed to load job details.");
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    loadDetail();
    return () => {
      isMounted = false;
    };
  }, [jobId, isOpen, accessToken]);

  if (!isOpen) return null;

  const handleToggleBookmark = async () => {
    if (!job || !accessToken) return;
    setIsSaving(true);
    try {
      if (job.isSaved) {
        await unsaveJob(job.id, accessToken);
        setJob({ ...job, isSaved: false });
        onBookmarkToggled?.(job.id, false);
      } else {
        await saveJob(job.id, accessToken);
        setJob({ ...job, isSaved: true });
        onBookmarkToggled?.(job.id, true);
      }
    } catch (err) {
      console.error("Bookmark toggle failed:", err);
    } finally {
      setIsSaving(false);
    }
  };

  const handleTrackApplication = async () => {
    if (!job || !accessToken) return;
    setIsTracking(true);
    setTrackFeedback(null);
    try {
      await createApplication(
        {
          jobId: job.id,
          status: "APPLIED",
          companyName: job.company,
          jobTitle: job.title,
          jobUrl: job.sourceUrl || undefined,
          jobDescription: job.description || undefined,
        },
        accessToken
      );
      setTrackStatus("tracked");
      setTrackFeedback("Tracked in your Applications pipeline!");
    } catch (err: unknown) {
      const msg = (err as Error).message || "Failed to track application.";
      if (
        msg.toLowerCase().includes("already tracked") ||
        msg.toLowerCase().includes("already exist") ||
        msg.toLowerCase().includes("duplicate") ||
        msg.includes("409")
      ) {
        setTrackStatus("exists");
        setTrackFeedback("Already tracked in your Applications pipeline.");
      } else {
        setTrackFeedback(msg);
      }
    } finally {
      setIsTracking(false);
    }
  };

  const formatSalary = (min: number | null, max: number | null, currency: string) => {
    if (!min && !max) return null;
    const formatNum = (n: number) => `$${n.toLocaleString()}`;
    if (min && max) return `${formatNum(min)} - ${formatNum(max)} ${currency}`;
    if (min) return `From ${formatNum(min)} ${currency}`;
    return `Up to ${formatNum(max!)} ${currency}`;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 lg:p-8">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      {/* Modal Dialog Body */}
      <div className="relative z-10 w-full max-w-3xl max-h-[90vh] flex flex-col rounded-2xl border border-slate-200 bg-white shadow-2xl overflow-hidden animate-in fade-in-0 zoom-in-95 duration-200">
        {/* Top Header */}
        <div className="flex items-start justify-between border-b border-slate-100 p-5 sm:p-6 bg-slate-50/50">
          <div className="space-y-1.5 flex-1 pr-4">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex items-center gap-1">
                <Building className="h-3.5 w-3.5 text-slate-400" />
                {job?.company || "Loading Company..."}
              </span>
              {job?.isSample && (
                <Badge variant="outline" className="text-[10px] bg-amber-50 text-amber-800 border-amber-200">
                  Sample Vacancy
                </Badge>
              )}
            </div>
            <h2 className="text-xl sm:text-2xl font-bold text-slate-900 leading-snug">
              {job?.title || "Job Details"}
            </h2>
            <div className="flex flex-wrap items-center gap-3 text-xs text-slate-600 pt-1">
              <span className="flex items-center gap-1">
                <MapPin className="h-3.5 w-3.5 text-slate-400" />
                {job?.location || "Location not specified"}
              </span>
              {job?.remoteType && (
                <Badge variant="secondary" className="text-[11px]">
                  {job.remoteType}
                </Badge>
              )}
              {job?.employmentType && (
                <Badge variant="outline" className="text-[11px]">
                  {job.employmentType.replace("_", " ")}
                </Badge>
              )}
              {job?.experienceLevel && (
                <Badge variant="info" className="text-[11px]">
                  {job.experienceLevel.replace("_", " ")}
                </Badge>
              )}
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center gap-2 shrink-0">
            {user && job && (
              <Button
                variant={job.isSaved ? "default" : "outline"}
                size="sm"
                onClick={handleToggleBookmark}
                disabled={isSaving}
                className={`gap-1.5 text-xs h-9 ${
                  job.isSaved ? "bg-blue-700 hover:bg-blue-800 text-white" : "border-slate-200"
                }`}
              >
                <Bookmark className={`h-4 w-4 ${job.isSaved ? "fill-current" : ""}`} />
                <span>{job.isSaved ? "Saved" : "Save Job"}</span>
              </Button>
            )}
            <button
              onClick={onClose}
              className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors"
              aria-label="Close dialog"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6 space-y-6">
          {isLoading && (
            <div className="py-16 text-center space-y-3">
              <Loader2 className="h-8 w-8 animate-spin text-blue-700 mx-auto" />
              <p className="text-xs text-muted-foreground">Loading job details and calculating match analysis...</p>
            </div>
          )}

          {error && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-xs text-rose-800 flex items-start gap-2.5">
              <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">Unable to display position details</p>
                <p className="mt-0.5 text-rose-700">{error}</p>
              </div>
            </div>
          )}

          {job && !isLoading && (
            <>
              {/* Salary & Meta Highlights */}
              {(job.salaryMin || job.salaryMax) && (
                <div className="rounded-xl border border-emerald-100 bg-emerald-50/60 p-4 flex items-center justify-between text-xs">
                  <div>
                    <span className="font-semibold text-emerald-900">Compensation Package:</span>
                    <span className="ml-2 font-bold text-emerald-700 text-sm">
                      {formatSalary(job.salaryMin, job.salaryMax, job.salaryCurrency)}
                    </span>
                  </div>
                  <Badge variant="outline" className="text-[10px] bg-white text-emerald-800 border-emerald-200">
                    Verified Budget
                  </Badge>
                </div>
              )}

              {/* Explainable Match Analysis Panel (when authenticated) */}
              {user ? (
                <div className="rounded-xl border border-blue-100 bg-blue-50/40 p-5 space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-700 text-white">
                        <Sparkles className="h-4 w-4" />
                      </div>
                      <div>
                        <h4 className="font-bold text-sm text-slate-900">Explainable Skill Match</h4>
                        <p className="text-[11px] text-muted-foreground">Deterministic alignment with your profile skills</p>
                      </div>
                    </div>

                    {job.match && job.match.hasRequiredSkills && job.match.matchPercentage !== null && (
                      <div className="text-right">
                        <span
                          className={`text-2xl font-black ${
                            job.match.matchPercentage >= 70
                              ? "text-emerald-700"
                              : job.match.matchPercentage >= 40
                              ? "text-blue-700"
                              : "text-slate-700"
                          }`}
                        >
                          {job.match.matchPercentage}%
                        </span>
                        <p className="text-[10px] text-muted-foreground font-medium">Skill Alignment</p>
                      </div>
                    )}
                  </div>

                  {job.match && (
                    <>
                      {/* Match percentage progress bar */}
                      {job.match.hasRequiredSkills && job.match.matchPercentage !== null && (
                        <div className="space-y-1">
                          <div className="h-2 w-full bg-slate-200 rounded-full overflow-hidden">
                            <div
                              className={`h-full rounded-full transition-all duration-500 ${
                                job.match.matchPercentage >= 70
                                  ? "bg-emerald-600"
                                  : job.match.matchPercentage >= 40
                                  ? "bg-blue-700"
                                  : "bg-slate-500"
                              }`}
                              style={{ width: `${job.match.matchPercentage}%` }}
                            />
                          </div>
                          <div className="flex justify-between text-[11px] text-slate-500">
                            <span>
                              {job.match.matchedRequiredCount} of {job.match.totalRequiredCount} required skills met
                            </span>
                            <span>{job.match.matchPercentage}% matched</span>
                          </div>
                        </div>
                      )}

                      {/* Plain-English Explanation */}
                      <p className="text-xs text-slate-700 leading-relaxed bg-white/80 p-3 rounded-lg border border-blue-100/80">
                        {job.match.explanation}
                      </p>

                      {/* Matched vs Missing Pills */}
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
                        {/* Matched Skills */}
                        <div className="space-y-1.5">
                          <span className="text-[11px] font-semibold uppercase tracking-wider text-emerald-800 flex items-center gap-1">
                            <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                            Matched Skills ({job.match.matchedRequiredCount})
                          </span>
                          <div className="flex flex-wrap gap-1.5">
                            {job.match.matchedRequiredSkills.length > 0 ? (
                              job.match.matchedRequiredSkills.map((name) => (
                                <span
                                  key={name}
                                  className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-100/80 text-emerald-800 text-[11px] font-medium"
                                >
                                  <CheckCircle2 className="h-3 w-3 text-emerald-600" />
                                  {name}
                                </span>
                              ))
                            ) : (
                              <span className="text-[11px] text-slate-400 italic">No matching skills yet</span>
                            )}
                          </div>
                        </div>

                        {/* Missing Skills */}
                        <div className="space-y-1.5">
                          <span className="text-[11px] font-semibold uppercase tracking-wider text-rose-800 flex items-center gap-1">
                            <AlertCircle className="h-3.5 w-3.5 text-rose-500" />
                            Missing Skills ({job.match.missingRequiredSkills.length})
                          </span>
                          <div className="flex flex-wrap gap-1.5">
                            {job.match.missingRequiredSkills.length > 0 ? (
                              job.match.missingRequiredSkills.map((name) => (
                                <span
                                  key={name}
                                  className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-rose-50 text-rose-800 border border-rose-200/60 text-[11px] font-medium"
                                >
                                  {name}
                                </span>
                              ))
                            ) : (
                              <span className="text-[11px] text-emerald-700 italic font-medium">
                                No missing required skills!
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              ) : (
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-xs text-slate-600 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <HelpCircle className="h-4 w-4 text-slate-400" />
                    <span>Sign in with your profile to inspect your personalized skill match percentage.</span>
                  </div>
                  <a href="/login">
                    <Button size="sm" variant="outline" className="text-xs h-8">
                      Sign In
                    </Button>
                  </a>
                </div>
              )}

              {/* Skills Breakdown */}
              <div className="space-y-3">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                  <Briefcase className="h-3.5 w-3.5 text-slate-400" />
                  Required Competencies
                </h4>
                <div className="flex flex-wrap gap-2">
                  {job.requiredSkills.length > 0 ? (
                    job.requiredSkills.map((s) => (
                      <Badge key={s.id} variant="secondary" className="text-xs font-medium py-1 px-2.5">
                        {s.name}
                        {s.category && <span className="ml-1 text-[10px] text-slate-400">({s.category})</span>}
                      </Badge>
                    ))
                  ) : (
                    <span className="text-xs text-slate-400 italic">No specific required skills listed.</span>
                  )}
                </div>

                {job.optionalSkills.length > 0 && (
                  <>
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 pt-2">
                      Bonus / Preferred Skills
                    </h4>
                    <div className="flex flex-wrap gap-2">
                      {job.optionalSkills.map((s) => (
                        <Badge key={s.id} variant="outline" className="text-xs text-slate-600 py-1 px-2.5">
                          {s.name}
                        </Badge>
                      ))}
                    </div>
                  </>
                )}
              </div>

              {/* Job Description */}
              <div className="space-y-2 pt-2 border-t border-slate-100">
                <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Role Overview & Responsibilities
                </h4>
                <div className="text-xs sm:text-sm text-slate-700 leading-relaxed whitespace-pre-line space-y-3">
                  {job.description || "No full description provided for this requisition."}
                </div>
              </div>
            </>
          )}
        </div>

        {trackFeedback && (
          <div
            className={`px-5 py-2 text-xs flex items-center justify-between border-t ${
              trackStatus === "tracked"
                ? "bg-emerald-50 text-emerald-800 border-emerald-200"
                : trackStatus === "exists"
                ? "bg-blue-50 text-blue-800 border-blue-200"
                : "bg-rose-50 text-rose-800 border-rose-200"
            }`}
          >
            <span>{trackFeedback}</span>
            <button
              onClick={() => setTrackFeedback(null)}
              className="text-slate-400 hover:text-slate-600 text-xs font-semibold ml-2"
            >
              &times;
            </button>
          </div>
        )}

        {/* Footer */}
        <div className="border-t border-slate-100 p-4 sm:p-5 bg-slate-50/60 flex items-center justify-between text-xs">
          <span className="text-slate-400">
            {job?.postedAt ? `Posted: ${new Date(job.postedAt).toLocaleDateString()}` : "Active Vacancy"}
          </span>

          <div className="flex items-center gap-3">
            {user && (
              <Button
                variant={trackStatus === "tracked" ? "secondary" : "default"}
                size="sm"
                onClick={handleTrackApplication}
                disabled={isTracking || trackStatus === "tracked" || trackStatus === "exists"}
                className={`text-xs h-9 gap-1.5 ${
                  trackStatus === "tracked"
                    ? "bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100"
                    : trackStatus === "exists"
                    ? "bg-slate-100 text-slate-600 border border-slate-200"
                    : "bg-blue-700 hover:bg-blue-800 text-white"
                }`}
              >
                {isTracking ? (
                  <>
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    Tracking...
                  </>
                ) : trackStatus === "tracked" ? (
                  <>
                    <Check className="h-3.5 w-3.5 text-emerald-600" />
                    Tracked in Pipeline
                  </>
                ) : trackStatus === "exists" ? (
                  <>
                    <Check className="h-3.5 w-3.5 text-slate-500" />
                    Already Tracked
                  </>
                ) : (
                  <>
                    <Send className="h-3.5 w-3.5" />
                    Track Application
                  </>
                )}
              </Button>
            )}

            {job?.sourceUrl && (
              <a
                href={job.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-blue-700 hover:text-blue-800 hover:underline"
              >
                Apply on External Site <ExternalLink className="h-3.5 w-3.5" />
              </a>
            )}
            <Button variant="outline" size="sm" onClick={onClose} className="text-xs h-9">
              Close
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};
