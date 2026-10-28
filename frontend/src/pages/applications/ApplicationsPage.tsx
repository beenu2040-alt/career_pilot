import React, { useState, useEffect } from "react";
import { useAuth } from "@/context/AuthContext";
import { Link } from "react-router-dom";
import {
  Send,
  Plus,
  Search,
  Kanban,
  List,
  Calendar,
  Clock,
  CheckCircle2,
  ExternalLink,
  TrendingUp,
  BarChart3,
  History,
  Trash2,
  Edit,
  AlertCircle,
  RefreshCw,
  Sparkles,
  Award,
  Layers,
} from "lucide-react";
import {
  ApplicationDto,
  ApplicationStatus,
  RemindersSummary,
  CareerAnalytics,
  JobSummaryResponse,
  fetchAllApplications,
  createApplication,
  updateApplication,
  updateApplicationStatus,
  deleteApplication,
  fetchReminders,
  fetchCareerAnalytics,
  fetchJobs,
} from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  LineChart,
  Line,
  CartesianGrid,
} from "recharts";

const STATUS_COLUMNS: { status: ApplicationStatus; label: string; color: string; badgeVariant: "default" | "secondary" | "success" | "warning" | "info" | "outline" }[] = [
  { status: "WISHLIST", label: "Wishlist", color: "bg-slate-100 text-slate-700 border-slate-300", badgeVariant: "secondary" },
  { status: "APPLIED", label: "Applied", color: "bg-blue-50 text-blue-700 border-blue-200", badgeVariant: "info" },
  { status: "SCREENING", label: "Screening", color: "bg-purple-50 text-purple-700 border-purple-200", badgeVariant: "info" },
  { status: "INTERVIEW", label: "Interview", color: "bg-amber-50 text-amber-800 border-amber-300", badgeVariant: "warning" },
  { status: "ASSESSMENT", label: "Assessment", color: "bg-orange-50 text-orange-800 border-orange-300", badgeVariant: "warning" },
  { status: "OFFER", label: "Offer", color: "bg-emerald-50 text-emerald-800 border-emerald-300", badgeVariant: "success" },
  { status: "REJECTED", label: "Rejected", color: "bg-red-50 text-red-700 border-red-200", badgeVariant: "outline" },
  { status: "WITHDRAWN", label: "Withdrawn", color: "bg-slate-100 text-slate-500 border-slate-200", badgeVariant: "outline" },
];


export const ApplicationsPage: React.FC = () => {
  const { accessToken } = useAuth();

  // State: View & Data
  const [viewMode, setViewMode] = useState<"kanban" | "list">("kanban");
  const [applications, setApplications] = useState<ApplicationDto[]>([]);
  const [reminders, setReminders] = useState<RemindersSummary | null>(null);
  const [analytics, setAnalytics] = useState<CareerAnalytics | null>(null);
  const [availableJobs, setAvailableJobs] = useState<JobSummaryResponse[]>([]);

  // State: Filters
  const [searchQuery, setSearchQuery] = useState("");
  const [filterStatus, setFilterStatus] = useState<string>("ALL");

  // State: Loading & Messages
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // State: Modals & Drawers
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showAnalyticsModal, setShowAnalyticsModal] = useState(false);
  const [selectedApp, setSelectedApp] = useState<ApplicationDto | null>(null);
  const [editingApp, setEditingApp] = useState<ApplicationDto | null>(null);

  // Form State: Create / Edit
  const [jobMode, setJobMode] = useState<"stored" | "manual">("manual");
  const [formJobId, setFormJobId] = useState("");
  const [formCompanyName, setFormCompanyName] = useState("");
  const [formJobTitle, setFormJobTitle] = useState("");
  const [formJobDescription, setFormJobDescription] = useState("");
  const [formJobUrl, setFormJobUrl] = useState("");
  const [formContactName, setFormContactName] = useState("");
  const [formContactEmail, setFormContactEmail] = useState("");
  const [formSalary, setFormSalary] = useState("");
  const [formStatus, setFormStatus] = useState<ApplicationStatus>("APPLIED");
  const [formAppliedAt, setFormAppliedAt] = useState(new Date().toISOString().split("T")[0]);
  const [formFollowUpDate, setFormFollowUpDate] = useState("");
  const [formInterviewAt, setFormInterviewAt] = useState("");
  const [formNotes, setFormNotes] = useState("");

  // Transition Note State
  const [statusTransitionNote, setStatusTransitionNote] = useState("");
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  useEffect(() => {
    if (accessToken) {
      loadAllData();
    }
  }, [accessToken]);

  const loadAllData = async () => {
    if (!accessToken) return;
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const [appsData, remindersData, analyticsData, jobsData] = await Promise.all([
        fetchAllApplications(accessToken).catch(() => []),
        fetchReminders(accessToken).catch(() => null),
        fetchCareerAnalytics(accessToken).catch(() => null),
        fetchJobs({ page: 0, size: 20 }, accessToken).catch(() => ({ content: [] })),
      ]);

      setApplications(appsData);
      setReminders(remindersData);
      setAnalytics(analyticsData);
      setAvailableJobs(jobsData.content || []);
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to load application pipeline.");
    } finally {
      setIsLoading(false);
    }
  };

  const refreshData = async () => {
    if (!accessToken) return;
    setIsRefreshing(true);
    try {
      const [appsData, remindersData, analyticsData] = await Promise.all([
        fetchAllApplications(accessToken),
        fetchReminders(accessToken),
        fetchCareerAnalytics(accessToken),
      ]);
      setApplications(appsData);
      setReminders(remindersData);
      setAnalytics(analyticsData);
      if (selectedApp) {
        const updatedSelected = appsData.find((a) => a.id === selectedApp.id);
        if (updatedSelected) setSelectedApp(updatedSelected);
      }
    } catch (err: any) {
      setErrorMessage(err.message || "Refresh failed.");
    } finally {
      setIsRefreshing(false);
    }
  };

  const handleOpenCreate = () => {
    setEditingApp(null);
    setJobMode(availableJobs.length > 0 ? "stored" : "manual");
    setFormJobId(availableJobs.length > 0 ? availableJobs[0].id : "");
    setFormCompanyName("");
    setFormJobTitle("");
    setFormJobDescription("");
    setFormJobUrl("");
    setFormContactName("");
    setFormContactEmail("");
    setFormSalary("");
    setFormStatus("APPLIED");
    setFormAppliedAt(new Date().toISOString().split("T")[0]);
    setFormFollowUpDate("");
    setFormInterviewAt("");
    setFormNotes("");
    setShowCreateModal(true);
  };

  const handleOpenEdit = (app: ApplicationDto) => {
    setEditingApp(app);
    setFormCompanyName(app.companyName);
    setFormJobTitle(app.jobTitle);
    setFormJobDescription(app.jobDescription || "");
    setFormJobUrl(app.jobUrl || "");
    setFormContactName(app.contactName || "");
    setFormContactEmail(app.contactEmail || "");
    setFormSalary(app.salaryOffered ? app.salaryOffered.toString() : "");
    setFormStatus(app.status);
    setFormAppliedAt(app.appliedAt ? new Date(app.appliedAt).toISOString().split("T")[0] : "");
    setFormFollowUpDate(app.nextFollowUpDate || "");
    setFormInterviewAt(app.interviewAt ? new Date(app.interviewAt).toISOString().slice(0, 16) : "");
    setFormNotes(app.notes || "");
    setShowCreateModal(true);
  };

  const handleSaveApplication = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accessToken) return;

    if (editingApp) {
      // UPDATE
      try {
        const payload = {
          companyName: formCompanyName,
          jobTitle: formJobTitle,
          jobDescription: formJobDescription || undefined,
          jobUrl: formJobUrl || undefined,
          contactName: formContactName || undefined,
          contactEmail: formContactEmail || undefined,
          salaryOffered: formSalary ? parseFloat(formSalary) : undefined,
          status: formStatus,
          appliedAt: formAppliedAt ? new Date(formAppliedAt).toISOString() : undefined,
          nextFollowUpDate: formFollowUpDate || undefined,
          interviewAt: formInterviewAt ? new Date(formInterviewAt).toISOString() : undefined,
          notes: formNotes || undefined,
        };
        const updated = await updateApplication(editingApp.id, payload, accessToken);
        setSuccessMessage(`Updated application for ${updated.companyName}!`);
        setShowCreateModal(false);
        refreshData();
        setTimeout(() => setSuccessMessage(null), 3000);
      } catch (err: any) {
        setErrorMessage(err.message || "Failed to update application.");
      }
    } else {
      // CREATE
      try {
        const payload = {
          jobId: jobMode === "stored" && formJobId ? formJobId : undefined,
          companyName: jobMode === "manual" ? formCompanyName : undefined,
          jobTitle: jobMode === "manual" ? formJobTitle : undefined,
          jobDescription: formJobDescription || undefined,
          jobUrl: formJobUrl || undefined,
          contactName: formContactName || undefined,
          contactEmail: formContactEmail || undefined,
          salaryOffered: formSalary ? parseFloat(formSalary) : undefined,
          status: formStatus,
          appliedAt: formAppliedAt ? new Date(formAppliedAt).toISOString() : undefined,
          nextFollowUpDate: formFollowUpDate || undefined,
          interviewAt: formInterviewAt ? new Date(formInterviewAt).toISOString() : undefined,
          notes: formNotes || undefined,
        };
        const created = await createApplication(payload, accessToken);
        setSuccessMessage(`Tracked application for ${created.companyName}!`);
        setShowCreateModal(false);
        refreshData();
        setTimeout(() => setSuccessMessage(null), 3000);
      } catch (err: any) {
        setErrorMessage(err.message || "Failed to create application.");
      }
    }
  };

  const handleStatusTransition = async (appId: string, newStatus: ApplicationStatus) => {
    if (!accessToken) return;
    setIsUpdatingStatus(true);
    try {
      const updated = await updateApplicationStatus(
        appId,
        { status: newStatus, notes: statusTransitionNote || undefined },
        accessToken
      );
      setStatusTransitionNote("");
      setSelectedApp(updated);
      setSuccessMessage(`Moved ${updated.companyName} to ${newStatus}!`);
      refreshData();
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to transition status.");
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleDelete = async (appId: string, company: string) => {
    if (!accessToken) return;
    if (!window.confirm(`Are you sure you want to delete application for ${company}? This will remove all history records.`)) {
      return;
    }
    try {
      await deleteApplication(appId, accessToken);
      if (selectedApp?.id === appId) setSelectedApp(null);
      setSuccessMessage(`Deleted application for ${company}.`);
      refreshData();
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to delete application.");
    }
  };

  // Filtered applications
  const filteredApps = applications.filter((app) => {
    const matchesQuery =
      searchQuery.trim() === "" ||
      app.companyName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      app.jobTitle.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus = filterStatus === "ALL" || app.status === filterStatus;
    return matchesQuery && matchesStatus;
  });

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[500px] space-y-4">
        <RefreshCw className="h-8 w-8 animate-spin text-primary" />
        <p className="text-sm font-medium text-muted-foreground">Loading Application Tracker & Analytics...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 1. HEADER BANNER */}
      <div className="rounded-2xl border border-slate-200/90 bg-gradient-to-r from-slate-900 via-blue-950 to-slate-900 p-6 sm:p-8 text-white shadow-md relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-500/20 border border-blue-400/30 text-blue-300">
                <Send className="h-5 w-5" />
              </span>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">Application Tracker</h1>
              <Badge variant="info" className="text-xs bg-blue-500/20 text-blue-300 border-blue-400/30">
                Phase 7 Active
              </Badge>
            </div>
            <p className="text-sm text-slate-300 max-w-2xl leading-relaxed">
              Full-lifecycle pipeline management, status timeline auditing, follow-up alerts, and conversion analytics.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Button
              onClick={() => setShowAnalyticsModal(true)}
              variant="outline"
              className="bg-white/10 hover:bg-white/20 text-white border-white/20 gap-2 text-xs"
            >
              <BarChart3 className="h-4 w-4 text-emerald-400" />
              Career Analytics
            </Button>
            <Button
              onClick={handleOpenCreate}
              className="bg-blue-600 hover:bg-blue-700 text-white font-semibold shadow-sm gap-2 text-xs"
            >
              <Plus className="h-4 w-4" />
              Track Application
            </Button>
          </div>
        </div>
      </div>

      {/* NOTIFICATIONS */}
      {errorMessage && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-900 flex items-start gap-3">
          <AlertCircle className="h-5 w-5 text-red-600 mt-0.5 shrink-0" />
          <div className="text-sm">
            <p className="font-semibold">Notice</p>
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

      {/* 2. REMINDERS & FOLLOW-UPS ALERT RIBBON */}
      {reminders && (reminders.overdue.length > 0 || reminders.dueToday.length > 0 || reminders.upcomingInterviews.length > 0) && (
        <div className="rounded-xl border border-amber-200 bg-amber-50/60 p-4 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-amber-900 font-bold text-sm">
              <Clock className="h-4 w-4 text-amber-600" />
              <span>Action Items & Reminders</span>
            </div>
            <span className="text-xs text-amber-800">In-App Scheduled Alerts</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
            {/* Overdue */}
            {reminders.overdue.length > 0 && (
              <div className="rounded-lg border border-red-200 bg-red-50 p-3 space-y-1">
                <div className="flex items-center justify-between font-bold text-red-800">
                  <span>Overdue Follow-ups</span>
                  <Badge variant="outline" className="bg-red-100 text-red-800 border-red-300 text-[10px]">
                    {reminders.overdue.length}
                  </Badge>
                </div>
                {reminders.overdue.slice(0, 2).map((a) => (
                  <p
                    key={a.id}
                    onClick={() => setSelectedApp(a)}
                    className="truncate cursor-pointer hover:underline text-red-700"
                  >
                    • {a.companyName} — Due {a.nextFollowUpDate}
                  </p>
                ))}
              </div>
            )}

            {/* Due Today */}
            {reminders.dueToday.length > 0 && (
              <div className="rounded-lg border border-amber-300 bg-white p-3 space-y-1">
                <div className="flex items-center justify-between font-bold text-amber-900">
                  <span>Due Today</span>
                  <Badge variant="outline" className="bg-amber-100 text-amber-800 border-amber-300 text-[10px]">
                    {reminders.dueToday.length}
                  </Badge>
                </div>
                {reminders.dueToday.slice(0, 2).map((a) => (
                  <p
                    key={a.id}
                    onClick={() => setSelectedApp(a)}
                    className="truncate cursor-pointer hover:underline text-slate-800"
                  >
                    • {a.companyName} — Follow up today
                  </p>
                ))}
              </div>
            )}

            {/* Upcoming Interviews */}
            {reminders.upcomingInterviews.length > 0 && (
              <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 space-y-1">
                <div className="flex items-center justify-between font-bold text-blue-900">
                  <span>Upcoming Interviews</span>
                  <Badge variant="outline" className="bg-blue-100 text-blue-800 border-blue-300 text-[10px]">
                    {reminders.upcomingInterviews.length}
                  </Badge>
                </div>
                {reminders.upcomingInterviews.slice(0, 2).map((a) => (
                  <p
                    key={a.id}
                    onClick={() => setSelectedApp(a)}
                    className="truncate cursor-pointer hover:underline text-blue-800"
                  >
                    • {a.companyName} — {new Date(a.interviewAt!).toLocaleDateString()}
                  </p>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* 3. PIPELINE QUICK METRICS SUMMARY */}
      {analytics && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <Card className="border-slate-200 p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-muted-foreground">Submitted</p>
                <p className="text-2xl font-black text-slate-900 mt-1">{analytics.submittedApplications}</p>
              </div>
              <div className="h-10 w-10 rounded-xl bg-blue-50 text-blue-700 flex items-center justify-center font-bold">
                <Send className="h-5 w-5" />
              </div>
            </div>
            <p className="text-[11px] text-muted-foreground mt-2">Active or completed submissions</p>
          </Card>

          <Card className="border-slate-200 p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-muted-foreground">Interview Rate</p>
                <p className="text-2xl font-black text-amber-700 mt-1">{analytics.interviewRate}%</p>
              </div>
              <div className="h-10 w-10 rounded-xl bg-amber-50 text-amber-700 flex items-center justify-center font-bold">
                <TrendingUp className="h-5 w-5" />
              </div>
            </div>
            <p className="text-[11px] text-muted-foreground mt-2">{analytics.interviewCount} distinct candidates interviewed</p>
          </Card>

          <Card className="border-slate-200 p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-muted-foreground">Offer Rate</p>
                <p className="text-2xl font-black text-emerald-700 mt-1">{analytics.offerRate}%</p>
              </div>
              <div className="h-10 w-10 rounded-xl bg-emerald-50 text-emerald-700 flex items-center justify-center font-bold">
                <Award className="h-5 w-5" />
              </div>
            </div>
            <p className="text-[11px] text-muted-foreground mt-2">{analytics.offerCount} formal offers received</p>
          </Card>

          <Card className="border-slate-200 p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-muted-foreground">In Pipeline</p>
                <p className="text-2xl font-black text-purple-700 mt-1">{analytics.activePipelineCount}</p>
              </div>
              <div className="h-10 w-10 rounded-xl bg-purple-50 text-purple-700 flex items-center justify-center font-bold">
                <Layers className="h-5 w-5" />
              </div>
            </div>
            <p className="text-[11px] text-muted-foreground mt-2">Active screening & interview rounds</p>
          </Card>
        </div>
      )}

      {/* 4. WORKSPACE CONTROLS & FILTER BAR */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white p-3.5 rounded-xl border border-slate-200 shadow-sm">
        <div className="flex items-center gap-2 flex-1 max-w-md">
          <div className="relative w-full">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search by company or role..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 rounded-lg border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="rounded-lg border border-slate-300 p-1.5 text-xs text-slate-700 focus:outline-none focus:ring-1 focus:ring-primary"
          >
            <option value="ALL">All Statuses</option>
            {STATUS_COLUMNS.map((col) => (
              <option key={col.status} value={col.status}>
                {col.label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-center gap-2 justify-end">
          <div className="flex rounded-lg border p-0.5 bg-slate-100">
            <button
              onClick={() => setViewMode("kanban")}
              className={`p-1.5 rounded-md text-xs font-semibold flex items-center gap-1 transition-all ${
                viewMode === "kanban" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500 hover:text-slate-900"
              }`}
            >
              <Kanban className="h-3.5 w-3.5" />
              Kanban
            </button>
            <button
              onClick={() => setViewMode("list")}
              className={`p-1.5 rounded-md text-xs font-semibold flex items-center gap-1 transition-all ${
                viewMode === "list" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500 hover:text-slate-900"
              }`}
            >
              <List className="h-3.5 w-3.5" />
              List
            </button>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={refreshData}
            disabled={isRefreshing}
            className="h-8 px-2 text-xs"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isRefreshing ? "animate-spin" : ""}`} />
          </Button>
        </div>
      </div>

      {/* 5. MAIN CONTENT: KANBAN OR LIST */}
      {applications.length === 0 ? (
        <Card className="border-dashed border-2 p-12 text-center">
          <CardContent className="space-y-4 max-w-md mx-auto">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-50 text-blue-600">
              <Send className="h-7 w-7" />
            </div>
            <h3 className="text-lg font-bold text-slate-900">Your Pipeline is Empty</h3>
            <p className="text-sm text-muted-foreground">
              Track job opportunities, interview rounds, and offers in one organized dashboard.
            </p>
            <div className="flex justify-center gap-3 pt-2">
              <Button onClick={handleOpenCreate} className="gap-2 text-xs">
                <Plus className="h-4 w-4" />
                Track Application
              </Button>
              <Link to="/jobs">
                <Button variant="outline" className="gap-2 text-xs">
                  <Search className="h-4 w-4" />
                  Browse Jobs
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>
      ) : viewMode === "kanban" ? (
        /* KANBAN BOARD */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 overflow-x-auto pb-4">
          {STATUS_COLUMNS.map((col) => {
            const columnApps = filteredApps.filter((a) => a.status === col.status);
            return (
              <div key={col.status} className="rounded-xl border border-slate-200 bg-slate-50/50 p-3 flex flex-col min-w-[260px] space-y-3">
                <div className="flex items-center justify-between pb-2 border-b border-slate-200">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-slate-800">{col.label}</span>
                    <Badge variant={col.badgeVariant} className="text-[10px] px-1.5 py-0">
                      {columnApps.length}
                    </Badge>
                  </div>
                </div>

                <div className="space-y-2.5 flex-1 min-h-[300px]">
                  {columnApps.length === 0 ? (
                    <div className="h-24 flex items-center justify-center border border-dashed rounded-lg text-[11px] text-muted-foreground">
                      No applications
                    </div>
                  ) : (
                    columnApps.map((app) => (
                      <div
                        key={app.id}
                        onClick={() => setSelectedApp(app)}
                        className="cursor-pointer rounded-lg border border-slate-200 bg-white p-3 shadow-xs hover:border-blue-400 hover:shadow-sm transition-all space-y-2"
                      >
                        <div className="flex items-start justify-between gap-1">
                          <p className="text-xs font-bold text-slate-900 truncate">{app.companyName}</p>
                          {app.nextFollowUpDate && (
                            <span className="flex items-center gap-1 text-[10px] text-amber-700 font-medium shrink-0">
                              <Clock className="h-3 w-3" />
                              {app.nextFollowUpDate}
                            </span>
                          )}
                        </div>

                        <p className="text-xs text-slate-600 truncate">{app.jobTitle}</p>

                        {app.interviewAt && (
                          <div className="flex items-center gap-1.5 text-[11px] font-semibold text-blue-700 bg-blue-50 px-2 py-1 rounded">
                            <Calendar className="h-3 w-3" />
                            Interview: {new Date(app.interviewAt).toLocaleDateString()}
                          </div>
                        )}

                        <div className="flex items-center justify-between pt-1 border-t text-[10px] text-muted-foreground">
                          <span>{app.appliedAt ? new Date(app.appliedAt).toLocaleDateString() : "Saved"}</span>
                          <span className="text-slate-400 font-mono">{app.history?.length || 1} logs</span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* LIST VIEW */
        <Card className="border-slate-200">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-700">
              <thead className="bg-slate-50 text-[11px] uppercase font-bold text-slate-500 border-b">
                <tr>
                  <th className="p-3">Company & Role</th>
                  <th className="p-3">Status</th>
                  <th className="p-3">Applied Date</th>
                  <th className="p-3">Follow-up</th>
                  <th className="p-3">Interview</th>
                  <th className="p-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredApps.map((app) => (
                  <tr
                    key={app.id}
                    onClick={() => setSelectedApp(app)}
                    className="hover:bg-slate-50/80 cursor-pointer transition-colors"
                  >
                    <td className="p-3">
                      <p className="font-bold text-slate-900">{app.companyName}</p>
                      <p className="text-muted-foreground">{app.jobTitle}</p>
                    </td>
                    <td className="p-3">
                      <Badge variant="outline" className="text-[10px] font-bold">
                        {app.status}
                      </Badge>
                    </td>
                    <td className="p-3 text-slate-600">
                      {app.appliedAt ? new Date(app.appliedAt).toLocaleDateString() : "—"}
                    </td>
                    <td className="p-3">
                      {app.nextFollowUpDate ? (
                        <span className="flex items-center gap-1 text-amber-700 font-medium">
                          <Clock className="h-3 w-3" />
                          {app.nextFollowUpDate}
                        </span>
                      ) : (
                        "—"
                      )}
                    </td>
                    <td className="p-3">
                      {app.interviewAt ? (
                        <span className="text-blue-700 font-medium">
                          {new Date(app.interviewAt).toLocaleDateString()}
                        </span>
                      ) : (
                        "—"
                      )}
                    </td>
                    <td className="p-3 text-right" onClick={(e) => e.stopPropagation()}>
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => handleOpenEdit(app)}
                          className="h-7 w-7 p-0 text-slate-500 hover:text-slate-900"
                        >
                          <Edit className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => handleDelete(app.id, app.companyName)}
                          className="h-7 w-7 p-0 text-red-500 hover:text-red-700"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* 6. APPLICATION DETAILS DRAWER / MODAL */}
      {selectedApp && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs">
          <div className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-150">
            {/* Header */}
            <div className="flex items-start justify-between border-b pb-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <h2 className="text-xl font-bold text-slate-900">{selectedApp.companyName}</h2>
                  <Badge variant="outline" className="text-xs font-bold uppercase">
                    {selectedApp.status}
                  </Badge>
                </div>
                <p className="text-sm font-semibold text-primary">{selectedApp.jobTitle}</p>
                {selectedApp.jobUrl && (
                  <a
                    href={selectedApp.jobUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1 text-xs text-blue-600 hover:underline pt-1"
                  >
                    <span>Job Posting Link</span>
                    <ExternalLink className="h-3 w-3" />
                  </a>
                )}
              </div>

              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    handleOpenEdit(selectedApp);
                    setSelectedApp(null);
                  }}
                  className="text-xs h-8 gap-1"
                >
                  <Edit className="h-3.5 w-3.5" />
                  Edit
                </Button>
                <button
                  onClick={() => setSelectedApp(null)}
                  className="text-slate-400 hover:text-slate-600 p-1"
                >
                  ✕
                </button>
              </div>
            </div>

            {/* Quick Advance Status Control */}
            <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase text-slate-700">Quick Advance Status</span>
                <span className="text-[11px] text-muted-foreground">Appends transition to audit history</span>
              </div>

              <div className="flex flex-wrap items-center gap-1.5">
                {STATUS_COLUMNS.map((col) => (
                  <button
                    key={col.status}
                    disabled={isUpdatingStatus || selectedApp.status === col.status}
                    onClick={() => handleStatusTransition(selectedApp.id, col.status)}
                    className={`px-2.5 py-1 rounded-md text-xs font-semibold transition-all ${
                      selectedApp.status === col.status
                        ? "bg-slate-900 text-white shadow-xs"
                        : "bg-white border hover:bg-slate-100 text-slate-700"
                    }`}
                  >
                    {col.label}
                  </button>
                ))}
              </div>

              <input
                type="text"
                placeholder="Optional transition note (e.g. Completed technical screen, HR follow-up)..."
                value={statusTransitionNote}
                onChange={(e) => setStatusTransitionNote(e.target.value)}
                className="w-full text-xs p-2 rounded-lg border border-slate-300 bg-white focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>

            {/* Metadata Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs">
              <div className="p-3 rounded-lg border bg-slate-50">
                <p className="text-slate-500 font-semibold text-[10px] uppercase">Application Date</p>
                <p className="font-bold text-slate-900 mt-1">
                  {selectedApp.appliedAt ? new Date(selectedApp.appliedAt).toLocaleDateString() : "Not submitted"}
                </p>
              </div>

              <div className="p-3 rounded-lg border bg-slate-50">
                <p className="text-slate-500 font-semibold text-[10px] uppercase">Next Follow-Up</p>
                <p className="font-bold text-slate-900 mt-1">
                  {selectedApp.nextFollowUpDate || "None set"}
                </p>
              </div>

              <div className="p-3 rounded-lg border bg-slate-50">
                <p className="text-slate-500 font-semibold text-[10px] uppercase">Interview Schedule</p>
                <p className="font-bold text-slate-900 mt-1">
                  {selectedApp.interviewAt ? new Date(selectedApp.interviewAt).toLocaleString() : "None"}
                </p>
              </div>

              {selectedApp.contactName && (
                <div className="p-3 rounded-lg border bg-slate-50">
                  <p className="text-slate-500 font-semibold text-[10px] uppercase">Recruiter / Contact</p>
                  <p className="font-bold text-slate-900 mt-1">{selectedApp.contactName}</p>
                </div>
              )}

              {selectedApp.contactEmail && (
                <div className="p-3 rounded-lg border bg-slate-50">
                  <p className="text-slate-500 font-semibold text-[10px] uppercase">Contact Email</p>
                  <p className="font-bold text-slate-900 mt-1">{selectedApp.contactEmail}</p>
                </div>
              )}

              {selectedApp.salaryOffered && (
                <div className="p-3 rounded-lg border bg-slate-50">
                  <p className="text-slate-500 font-semibold text-[10px] uppercase">Target / Offered Salary</p>
                  <p className="font-bold text-slate-900 mt-1">${selectedApp.salaryOffered.toLocaleString()}</p>
                </div>
              )}
            </div>

            {/* Notes Section */}
            {selectedApp.notes && (
              <div className="space-y-1 text-xs">
                <h4 className="font-bold uppercase tracking-wider text-slate-500 text-[10px]">Notes</h4>
                <div className="rounded-lg border bg-slate-50 p-3 text-slate-800 whitespace-pre-wrap font-sans">
                  {selectedApp.notes}
                </div>
              </div>
            )}

            {/* Status History Timeline */}
            <div className="space-y-3 pt-2 border-t">
              <div className="flex items-center gap-2">
                <History className="h-4 w-4 text-slate-500" />
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700">
                  Status Transition Audit History
                </h4>
              </div>

              <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                {selectedApp.history && selectedApp.history.length > 0 ? (
                  selectedApp.history.map((h) => (
                    <div key={h.id} className="rounded-lg border border-slate-200 bg-white p-2.5 text-xs space-y-1">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-1.5 font-bold">
                          {h.previousStatus ? (
                            <>
                              <Badge variant="outline" className="text-[10px]">
                                {h.previousStatus}
                              </Badge>
                              <span>→</span>
                            </>
                          ) : null}
                          <Badge variant="secondary" className="text-[10px]">
                            {h.newStatus}
                          </Badge>
                        </div>
                        <span className="text-[11px] text-muted-foreground">
                          {new Date(h.createdAt).toLocaleString()}
                        </span>
                      </div>
                      {h.notes && <p className="text-slate-600 pl-1 text-[11px]">{h.notes}</p>}
                    </div>
                  ))
                ) : (
                  <p className="text-xs text-muted-foreground">No historical transitions recorded yet.</p>
                )}
              </div>
            </div>

            <div className="flex justify-between items-center pt-3 border-t">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleDelete(selectedApp.id, selectedApp.companyName)}
                className="text-xs text-red-600 hover:bg-red-50 hover:text-red-700"
              >
                Delete Application
              </Button>
              <Button size="sm" onClick={() => setSelectedApp(null)} className="text-xs">
                Close
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* 7. CREATE / EDIT APPLICATION MODAL */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs">
          <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-bold text-slate-900">
                {editingApp ? `Edit Application (${editingApp.companyName})` : "Track New Job Application"}
              </h3>
              <button onClick={() => setShowCreateModal(false)} className="text-slate-400 hover:text-slate-600">
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveApplication} className="space-y-3.5 text-xs">
              {!editingApp && (
                <div className="space-y-1">
                  <label className="font-semibold text-slate-700">Source Job</label>
                  <div className="flex rounded-lg border p-1 bg-slate-50 gap-1">
                    <button
                      type="button"
                      onClick={() => setJobMode("stored")}
                      className={`flex-1 py-1 rounded-md font-semibold text-xs ${
                        jobMode === "stored" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"
                      }`}
                    >
                      From Saved / Catalog Jobs
                    </button>
                    <button
                      type="button"
                      onClick={() => setJobMode("manual")}
                      className={`flex-1 py-1 rounded-md font-semibold text-xs ${
                        jobMode === "manual" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"
                      }`}
                    >
                      Enter Manually
                    </button>
                  </div>
                </div>
              )}

              {!editingApp && jobMode === "stored" ? (
                <div className="space-y-1">
                  <label className="font-semibold text-slate-700">Select Job Posting</label>
                  <select
                    value={formJobId}
                    onChange={(e) => setFormJobId(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 p-2 text-xs text-slate-900"
                  >
                    {availableJobs.map((j) => (
                      <option key={j.id} value={j.id}>
                        {j.company} — {j.title}
                      </option>
                    ))}
                  </select>
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <label className="font-semibold text-slate-700">Company Name *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Netflix, Stripe"
                      value={formCompanyName}
                      onChange={(e) => setFormCompanyName(e.target.value)}
                      className="w-full rounded-lg border border-slate-300 p-2 text-xs"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="font-semibold text-slate-700">Job Title *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Senior Backend Engineer"
                      value={formJobTitle}
                      onChange={(e) => setFormJobTitle(e.target.value)}
                      className="w-full rounded-lg border border-slate-300 p-2 text-xs"
                    />
                  </div>
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="font-semibold text-slate-700">Status</label>
                  <select
                    value={formStatus}
                    onChange={(e) => setFormStatus(e.target.value as ApplicationStatus)}
                    className="w-full rounded-lg border border-slate-300 p-2 text-xs"
                  >
                    {STATUS_COLUMNS.map((col) => (
                      <option key={col.status} value={col.status}>
                        {col.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-1">
                  <label className="font-semibold text-slate-700">Application Date</label>
                  <input
                    type="date"
                    value={formAppliedAt}
                    onChange={(e) => setFormAppliedAt(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 p-2 text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="font-semibold text-slate-700">Next Follow-Up Date</label>
                  <input
                    type="date"
                    value={formFollowUpDate}
                    onChange={(e) => setFormFollowUpDate(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 p-2 text-xs"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-semibold text-slate-700">Interview Date & Time</label>
                  <input
                    type="datetime-local"
                    value={formInterviewAt}
                    onChange={(e) => setFormInterviewAt(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 p-2 text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div className="space-y-1">
                  <label className="font-semibold text-slate-700">Job URL</label>
                  <input
                    type="url"
                    placeholder="https://..."
                    value={formJobUrl}
                    onChange={(e) => setFormJobUrl(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 p-2 text-xs"
                  />
                </div>
                <div className="space-y-1">
                  <label className="font-semibold text-slate-700">Contact Name</label>
                  <input
                    type="text"
                    placeholder="Recruiter Name"
                    value={formContactName}
                    onChange={(e) => setFormContactName(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 p-2 text-xs"
                  />
                </div>
                <div className="space-y-1">
                  <label className="font-semibold text-slate-700">Target Salary ($)</label>
                  <input
                    type="number"
                    placeholder="140000"
                    value={formSalary}
                    onChange={(e) => setFormSalary(e.target.value)}
                    className="w-full rounded-lg border border-slate-300 p-2 text-xs"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="font-semibold text-slate-700">Notes & Strategy</label>
                <textarea
                  rows={3}
                  placeholder="Referral contacts, prep notes, salary benchmarks..."
                  value={formNotes}
                  onChange={(e) => setFormNotes(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 p-2 text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button type="button" variant="outline" size="sm" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" size="sm" className="bg-primary text-white">
                  {editingApp ? "Save Changes" : "Create Application"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 8. CAREER ANALYTICS MODAL WITH RECHARTS */}
      {showAnalyticsModal && analytics && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs">
          <div className="w-full max-w-3xl max-h-[90vh] overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl space-y-6 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <BarChart3 className="h-5 w-5 text-emerald-600" />
                <h3 className="text-lg font-bold text-slate-900">Career & Application Analytics</h3>
              </div>
              <button onClick={() => setShowAnalyticsModal(false)} className="text-slate-400 hover:text-slate-600">
                ✕
              </button>
            </div>

            {/* Formula & Definitions Card */}
            <div className="rounded-xl border border-blue-200 bg-blue-50/50 p-4 text-xs text-blue-950 space-y-1">
              <p className="font-bold flex items-center gap-1.5">
                <Sparkles className="h-4 w-4 text-blue-600" />
                Mathematical Formulas & Accuracy Disclaimers
              </p>
              <p className="text-blue-900">
                • <strong>Interview Rate</strong> = Distinct applications reaching screening/interview ({analytics.interviewCount}) ÷ Submitted applications ({analytics.submittedApplications}) = <strong>{analytics.interviewRate}%</strong>.
              </p>
              <p className="text-blue-900">
                • <strong>Offer Rate</strong> = Distinct applications reaching offer ({analytics.offerCount}) ÷ Submitted applications ({analytics.submittedApplications}) = <strong>{analytics.offerRate}%</strong>.
              </p>
              <p className="text-blue-800 text-[11px] pt-1">
                * Wishlist applications are excluded from submitted denominators. Repeated transitions count once per distinct application.
              </p>
            </div>

            {/* Recharts: Status Distribution Bar Chart */}
            <div className="space-y-2">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700">
                Application Pipeline Distribution
              </h4>
              <div className="h-64 w-full border rounded-xl p-3 bg-slate-50/50">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={Object.entries(analytics.statusDistribution).map(([status, count]) => ({
                      status,
                      count,
                    }))}
                  >
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                    <XAxis dataKey="status" tick={{ fontSize: 10 }} />
                    <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                    <Tooltip />
                    <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Recharts: Timeline Velocity Chart */}
            {analytics.timeline.length > 0 && (
              <div className="space-y-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700">
                  Submission Velocity Over Time
                </h4>
                <div className="h-48 w-full border rounded-xl p-3 bg-slate-50/50">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={analytics.timeline}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                      <XAxis dataKey="dateLabel" tick={{ fontSize: 10 }} />
                      <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                      <Tooltip />
                      <Line type="monotone" dataKey="count" stroke="#10b981" strokeWidth={2} dot={{ r: 4 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </div>
            )}

            <div className="flex justify-end pt-2 border-t">
              <Button size="sm" onClick={() => setShowAnalyticsModal(false)}>
                Done
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
