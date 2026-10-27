import React from "react";
import { Link, Outlet } from "react-router-dom";
import { Compass, ArrowRight, ShieldCheck, Database, Layers, Cpu } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/context/AuthContext";

export const PublicLayout: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="flex min-h-screen flex-col bg-white text-slate-900 selection:bg-blue-100 selection:text-blue-900">
      {/* Public Navigation Header */}
      <header className="sticky top-0 z-40 w-full border-b border-slate-200/80 bg-white/90 backdrop-blur supports-[backdrop-filter]:bg-white/80">
        <div className="container mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2.5 font-bold text-lg tracking-tight">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-700 text-white shadow-sm">
              <Compass className="h-5 w-5" />
            </div>
            <span>CareerPilot <span className="text-blue-700">AI</span></span>
          </Link>

          {/* Navigation Anchors */}
          <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-slate-600">
            <a href="#features" className="hover:text-slate-900 transition-colors">
              Features
            </a>
            <a href="#how-it-works" className="hover:text-slate-900 transition-colors">
              How It Works
            </a>
            <a href="#preview" className="hover:text-slate-900 transition-colors">
              Platform Preview
            </a>
          </nav>

          {/* Action CTAs */}
          <div className="flex items-center gap-3">
            {user ? (
              <Link to="/dashboard">
                <Button className="gap-2">
                  Go to Dashboard <ArrowRight className="h-4 w-4" />
                </Button>
              </Link>
            ) : (
              <>
                <Link to="/login">
                  <Button variant="ghost" size="sm" className="text-slate-600 hover:text-slate-900">
                    Sign In
                  </Button>
                </Link>
                <Link to="/register">
                  <Button size="sm" className="gap-1.5 bg-blue-700 hover:bg-blue-800 text-white shadow-sm">
                    Get Started <ArrowRight className="h-3.5 w-3.5" />
                  </Button>
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      {/* Main Page Content */}
      <main className="flex-1">
        <Outlet />
      </main>

      {/* Marketing Footer */}
      <footer className="border-t border-slate-200 bg-slate-50/70 py-12 text-sm text-slate-500">
        <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6 pb-8 border-b border-slate-200">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-700 text-white">
                  <Compass className="h-4 w-4" />
                </div>
                <span className="font-bold text-base text-slate-900">CareerPilot AI</span>
              </div>
              <p className="text-xs text-slate-500 max-w-md">
                Production-minded career development platform. Map competencies, identify actionable skill gaps, and navigate career transitions.
              </p>
            </div>

            <div className="flex flex-wrap gap-6 text-xs text-slate-600">
              <Link to="/dashboard" className="hover:text-blue-700 transition-colors">Dashboard</Link>
              <Link to="/profile" className="hover:text-blue-700 transition-colors">Profile</Link>
              <Link to="/login" className="hover:text-blue-700 transition-colors">Sign In</Link>
              <Link to="/register" className="hover:text-blue-700 transition-colors">Create Account</Link>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-6 text-xs text-slate-400">
            <p>© {new Date().getFullYear()} CareerPilot AI. All rights reserved.</p>
            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1">
                <Layers className="h-3.5 w-3.5 text-blue-600" /> React 18 + Vite
              </span>
              <span className="flex items-center gap-1">
                <Cpu className="h-3.5 w-3.5 text-emerald-600" /> Spring Boot 3
              </span>
              <span className="flex items-center gap-1">
                <Database className="h-3.5 w-3.5 text-teal-600" /> Supabase PostgreSQL
              </span>
              <span className="flex items-center gap-1">
                <ShieldCheck className="h-3.5 w-3.5 text-indigo-600" /> JWT Auth
              </span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};
