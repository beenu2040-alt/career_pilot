import React, { useState, useEffect } from "react";
import { Link, useLocation, useNavigate, Outlet } from "react-router-dom";
import {
  LayoutDashboard,
  Search,
  FileText,
  Compass,
  Wand2,
  Send,
  User as UserIcon,
  Settings,
  Menu,
  X,
  Activity,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  LogOut,
  ChevronRight,
  ChevronLeft,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { DropdownMenu } from "@/components/ui/dropdown-menu";
import { useAuth } from "@/context/AuthContext";
import { fetchBackendHealth, type HealthResponse } from "@/lib/api";

interface NavItem {
  name: string;
  path: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: string;
  badgeVariant?: "default" | "secondary" | "success" | "warning" | "info" | "outline";
}

const navItems: NavItem[] = [
  { name: "Overview", path: "/dashboard", icon: LayoutDashboard },
  { name: "Find Jobs", path: "/jobs", icon: Search },
  { name: "Resume Analyzer", path: "/resume-analyzer", icon: FileText },
  { name: "Career Roadmap", path: "/roadmap", icon: Compass },
  { name: "Resume Tailor", path: "/resume-tailor", icon: Wand2, badge: "AI", badgeVariant: "info" },
  { name: "Applications", path: "/applications", icon: Send, badge: "Live", badgeVariant: "success" },
  { name: "My Profile", path: "/profile", icon: UserIcon },
  { name: "Settings", path: "/settings", icon: Settings, badge: "Preview", badgeVariant: "outline" },
];

export const DashboardLayout: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, signOut } = useAuth();

  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [healthStatus, setHealthStatus] = useState<"checking" | "connected" | "disconnected">("checking");
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Close mobile drawer on route change
  useEffect(() => {
    setIsMobileOpen(false);
  }, [location.pathname]);

  const checkHealth = async () => {
    setIsRefreshing(true);
    try {
      const data = await fetchBackendHealth();
      setHealth(data);
      setHealthStatus("connected");
    } catch {
      setHealth(null);
      setHealthStatus("disconnected");
    } finally {
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    checkHealth();
    const interval = setInterval(checkHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  const currentNavItem: NavItem = navItems.find((item) => item.path === location.pathname) || {
    name: "Dashboard",
    path: "/dashboard",
    icon: LayoutDashboard,
  };

  const userMenuItems = [
    {
      label: "My Profile",
      icon: <UserIcon className="h-4 w-4" />,
      onClick: () => navigate("/profile"),
    },
    {
      label: "Account Settings",
      icon: <Settings className="h-4 w-4" />,
      onClick: () => navigate("/settings"),
    },
    {
      label: "Sign Out",
      icon: <LogOut className="h-4 w-4 text-rose-500" />,
      destructive: true,
      divider: true,
      onClick: async () => {
        await signOut();
        navigate("/login");
      },
    },
  ];

  return (
    <div className="flex min-h-screen bg-slate-50/60 dark:bg-slate-950 text-foreground">
      {/* Mobile Drawer Backdrop */}
      {isMobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/40 backdrop-blur-sm lg:hidden transition-opacity"
          onClick={() => setIsMobileOpen(false)}
        />
      )}

      {/* Sidebar: Desktop + Mobile Drawer */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-50 flex flex-col border-r border-slate-200/80 bg-white dark:border-slate-800 dark:bg-slate-900 transition-all duration-200 ease-in-out ${
          isMobileOpen ? "translate-x-0 w-64 shadow-2xl" : "-translate-x-full lg:translate-x-0"
        } ${isCollapsed ? "lg:w-20" : "lg:w-64"}`}
      >
        {/* Brand Header */}
        <div className="flex h-16 items-center justify-between px-4 border-b border-slate-100 dark:border-slate-800">
          <Link to="/" className="flex items-center gap-2.5 overflow-hidden">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-blue-700 text-white shadow-sm">
              <Compass className="h-5 w-5" />
            </div>
            {!isCollapsed && (
              <span className="font-bold text-base tracking-tight text-slate-900 dark:text-white truncate">
                CareerPilot <span className="text-blue-700 dark:text-blue-400">AI</span>
              </span>
            )}
          </Link>

          {/* Desktop collapse toggle */}
          <button
            onClick={() => setIsCollapsed(!isCollapsed)}
            className="hidden lg:flex h-7 w-7 items-center justify-center rounded-md text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800"
            title={isCollapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            {isCollapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </button>

          {/* Mobile close toggle */}
          <button
            onClick={() => setIsMobileOpen(false)}
            className="lg:hidden h-8 w-8 flex items-center justify-center rounded-md text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* User Mini Profile (when expanded) */}
        {!isCollapsed && user && (
          <div className="px-4 py-3 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
            <div className="flex items-center gap-3">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-100 text-blue-700 font-semibold text-xs dark:bg-blue-900 dark:text-blue-200">
                {user.email ? user.email.charAt(0).toUpperCase() : "U"}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold text-slate-800 dark:text-slate-200 truncate">
                  {user.user_metadata?.full_name || user.email?.split("@")[0] || "User"}
                </p>
                <p className="text-[11px] text-muted-foreground truncate">{user.email}</p>
              </div>
            </div>
          </div>
        )}

        {/* Navigation Items */}
        <nav className="flex-1 space-y-1 px-3 py-4 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;

            return (
              <Link
                key={item.path}
                to={item.path}
                title={isCollapsed ? item.name : undefined}
                className={`group flex items-center gap-3 rounded-lg px-3 py-2.5 text-xs font-medium transition-colors ${
                  isActive
                    ? "bg-blue-50 text-blue-700 font-semibold dark:bg-blue-950/60 dark:text-blue-300"
                    : "text-slate-600 hover:bg-slate-100/80 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
                } ${isCollapsed ? "justify-center" : ""}`}
              >
                <Icon
                  className={`h-4 w-4 shrink-0 transition-colors ${
                    isActive ? "text-blue-700 dark:text-blue-400" : "text-slate-400 group-hover:text-slate-600 dark:group-hover:text-slate-300"
                  }`}
                />
                {!isCollapsed && (
                  <span className="flex-1 truncate">{item.name}</span>
                )}
                {!isCollapsed && item.badge && (
                  <Badge variant={item.badgeVariant || "secondary"} className="text-[10px] px-1.5 py-0">
                    {item.badge}
                  </Badge>
                )}
              </Link>
            );
          })}
        </nav>

        {/* Bottom Status & Logout Section */}
        <div className="p-3 border-t border-slate-100 dark:border-slate-800 space-y-2">
          {!isCollapsed && (
            <div className="rounded-lg bg-slate-50 p-2.5 border border-slate-100 dark:bg-slate-800/40 dark:border-slate-800 text-[11px] flex items-center justify-between">
              <span className="text-muted-foreground flex items-center gap-1.5 font-medium">
                <Activity className="h-3 w-3 text-slate-400" /> API Status
              </span>
              <span className="flex items-center gap-1">
                {healthStatus === "connected" && (
                  <span className="flex items-center gap-1 text-emerald-600 font-semibold">
                    <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
                    Online
                  </span>
                )}
                {healthStatus === "disconnected" && (
                  <span className="flex items-center gap-1 text-amber-600 font-semibold">
                    <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
                    Offline
                  </span>
                )}
                {healthStatus === "checking" && (
                  <span className="text-slate-400 font-semibold">Connecting...</span>
                )}
              </span>
            </div>
          )}

          <button
            onClick={async () => {
              await signOut();
              navigate("/login");
            }}
            className={`flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-xs font-medium text-slate-600 hover:bg-rose-50 hover:text-rose-600 dark:text-slate-400 dark:hover:bg-rose-950/40 dark:hover:text-rose-400 transition-colors ${
              isCollapsed ? "justify-center" : ""
            }`}
            title="Sign Out"
          >
            <LogOut className="h-4 w-4 shrink-0 text-slate-400 group-hover:text-rose-600" />
            {!isCollapsed && <span>Sign Out</span>}
          </button>
        </div>
      </aside>

      {/* Main Container Area */}
      <div
        className={`flex flex-1 flex-col transition-all duration-200 ease-in-out ${
          isCollapsed ? "lg:pl-20" : "lg:pl-64"
        }`}
      >
        {/* Top Header */}
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200/80 bg-white/95 backdrop-blur px-4 sm:px-6 lg:px-8 dark:border-slate-800 dark:bg-slate-900/95">
          <div className="flex items-center gap-3">
            {/* Mobile Hamburger Button */}
            <button
              onClick={() => setIsMobileOpen(true)}
              className="lg:hidden flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 dark:border-slate-700 dark:text-slate-300"
              aria-label="Open navigation menu"
            >
              <Menu className="h-5 w-5" />
            </button>

            {/* Breadcrumb / Page Title */}
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider hidden sm:inline">
                CareerPilot
              </span>
              <span className="text-slate-300 hidden sm:inline">/</span>
              <h1 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
                {currentNavItem.name}
                {currentNavItem.badge && (
                  <Badge variant="outline" className="text-[10px] py-0 font-normal">
                    {currentNavItem.badge}
                  </Badge>
                )}
              </h1>
            </div>
          </div>

          {/* Right Header Utilities */}
          <div className="flex items-center gap-3">
            {/* Live Backend Connection Indicator */}
            <div className="hidden sm:flex items-center gap-1.5 text-xs text-muted-foreground border border-slate-200 dark:border-slate-800 rounded-full px-2.5 py-1">
              {healthStatus === "connected" ? (
                <>
                  <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                  <span className="text-slate-700 dark:text-slate-300 font-medium">Backend: {health?.status || "UP"}</span>
                </>
              ) : healthStatus === "checking" ? (
                <>
                  <Activity className="h-3.5 w-3.5 text-slate-400 animate-spin" />
                  <span>Connecting...</span>
                </>
              ) : (
                <>
                  <AlertCircle className="h-3.5 w-3.5 text-amber-600" />
                  <span className="text-amber-700">Backend Offline</span>
                </>
              )}
              <button
                onClick={checkHealth}
                disabled={isRefreshing}
                className="ml-1 text-slate-400 hover:text-slate-600 focus:outline-none"
                title="Ping backend health endpoint"
              >
                <RefreshCw className={`h-3 w-3 ${isRefreshing ? "animate-spin" : ""}`} />
              </button>
            </div>

            {/* User Profile Dropdown Menu */}
            {user && (
              <DropdownMenu
                trigger={
                  <button
                    type="button"
                    className="flex items-center gap-2 rounded-full p-1 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-600"
                  >
                    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-700 text-white font-semibold text-xs shadow-sm">
                      {user.email ? user.email.charAt(0).toUpperCase() : "U"}
                    </div>
                  </button>
                }
                items={userMenuItems}
              />
            )}
          </div>
        </header>

        {/* Content View Outlet */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
