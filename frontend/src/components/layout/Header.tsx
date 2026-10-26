import React, { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { Compass, Activity, CheckCircle2, AlertCircle, RefreshCw } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { fetchBackendHealth, type HealthResponse } from "@/lib/api";

export const Header: React.FC = () => {
  const location = useLocation();
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [status, setStatus] = useState<"checking" | "connected" | "disconnected">("checking");
  const [isRefreshing, setIsRefreshing] = useState(false);

  const checkHealth = async () => {
    setIsRefreshing(true);
    try {
      const data = await fetchBackendHealth();
      setHealth(data);
      setStatus("connected");
    } catch {
      setHealth(null);
      setStatus("disconnected");
    } finally {
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    checkHealth();
    // Periodically poll every 30 seconds
    const interval = setInterval(checkHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  const navLinks = [
    { name: "Overview", path: "/" },
    { name: "Resume Studio", path: "/resume", badge: "Phase 1" },
    { name: "Career Roadmap", path: "/roadmap", badge: "Phase 2" },
    { name: "Application Tracker", path: "/applications", badge: "Phase 3" },
  ];

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-8">
        <div className="flex items-center gap-6">
          <Link to="/" className="flex items-center gap-2.5 font-bold text-lg tracking-tight">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 text-white shadow-md">
              <Compass className="h-5 w-5" />
            </div>
            <span>CareerPilot <span className="text-blue-600">AI</span></span>
            <Badge variant="outline" className="ml-1 text-[10px] font-mono font-medium">v0.1.0-alpha</Badge>
          </Link>

          <nav className="hidden md:flex items-center gap-1 text-sm font-medium">
            {navLinks.map((link) => {
              const isActive = location.pathname === link.path;
              return (
                <Link
                  key={link.path}
                  to={link.path}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md transition-colors ${
                    isActive
                      ? "bg-secondary text-foreground font-semibold"
                      : "text-muted-foreground hover:bg-secondary/50 hover:text-foreground"
                  }`}
                >
                  {link.name}
                  {link.badge && (
                    <span className="text-[10px] px-1.5 py-0.5 rounded bg-muted text-muted-foreground border">
                      {link.badge}
                    </span>
                  )}
                </Link>
              );
            })}
          </nav>
        </div>

        <div className="flex items-center gap-3">
          <div className="hidden sm:flex items-center gap-2 text-xs">
            <span className="text-muted-foreground">Backend API:</span>
            {status === "checking" && (
              <Badge variant="secondary" className="gap-1 animate-pulse">
                <Activity className="h-3 w-3 text-muted-foreground" />
                Connecting...
              </Badge>
            )}
            {status === "connected" && (
              <Badge variant="success" className="gap-1">
                <CheckCircle2 className="h-3 w-3 text-emerald-600" />
                {health?.status || "UP"}
              </Badge>
            )}
            {status === "disconnected" && (
              <Badge variant="warning" className="gap-1">
                <AlertCircle className="h-3 w-3 text-amber-600" />
                Offline
              </Badge>
            )}
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-muted-foreground hover:text-foreground"
              onClick={checkHealth}
              disabled={isRefreshing}
              title="Ping backend health endpoint"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${isRefreshing ? "animate-spin" : ""}`} />
            </Button>
          </div>
        </div>
      </div>
    </header>
  );
};
