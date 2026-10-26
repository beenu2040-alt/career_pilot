import React, { useState } from "react";
import { 
  Server, 
  Layers, 
  Database, 
  ShieldCheck, 
  Terminal, 
  CheckCircle2, 
  Sparkles,
  ArrowRight,
  Code2,
  RefreshCw,
  Clock
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { fetchBackendHealth, type HealthResponse } from "@/lib/api";

export const HomePage: React.FC = () => {
  const [healthResult, setHealthResult] = useState<HealthResponse | null>(null);
  const [healthError, setHealthError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const handleTestHealth = async () => {
    setIsLoading(true);
    setHealthError(null);
    try {
      const data = await fetchBackendHealth();
      setHealthResult(data);
    } catch (err) {
      setHealthError((err as Error).message);
      setHealthResult(null);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="container mx-auto max-w-7xl px-4 py-12 sm:px-8">
      {/* Hero Section */}
      <div className="flex flex-col items-center text-center space-y-4 max-w-3xl mx-auto mb-16">
        <Badge variant="outline" className="px-3 py-1 text-xs font-medium border-blue-200 bg-blue-50 text-blue-700">
          <Sparkles className="h-3 w-3 mr-1 text-blue-500 inline" /> Phase 0 Foundation Initialized
        </Badge>
        <h1 className="text-4xl font-extrabold tracking-tight sm:text-5xl lg:text-6xl text-slate-900">
          CareerPilot <span className="bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">AI</span>
        </h1>
        <p className="text-lg text-muted-foreground leading-relaxed">
          Production-minded career development SaaS foundation. Built with modern, modular architectures
          featuring React & Vite, Java Spring Boot 3, and Supabase PostgreSQL.
        </p>
        <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
          <Button onClick={handleTestHealth} disabled={isLoading} className="gap-2">
            <RefreshCw className={`h-4 w-4 ${isLoading ? "animate-spin" : ""}`} />
            Test Backend Health
          </Button>
          <a href="#architecture">
            <Button variant="outline" className="gap-1.5">
              Architecture Overview <ArrowRight className="h-4 w-4" />
            </Button>
          </a>
        </div>
      </div>

      {/* Health Check Live Interactive Panel */}
      <div className="mb-14">
        <Card className="border-slate-200 shadow-sm overflow-hidden">
          <div className="bg-slate-900 text-slate-100 px-6 py-3 flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs font-mono">
              <Terminal className="h-4 w-4 text-emerald-400" />
              <span>LIVE API VERIFICATION — GET /api/health</span>
            </div>
            <span className="text-xs text-slate-400 font-mono">origin: localhost:5173</span>
          </div>
          <CardContent className="p-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <h3 className="font-semibold text-lg text-slate-900">Spring Boot Health Diagnostic</h3>
                <p className="text-sm text-muted-foreground">
                  Validates real-time CORS origin negotiation, JSON serialization, and backend connectivity.
                </p>
              </div>
              <Button 
                onClick={handleTestHealth} 
                disabled={isLoading} 
                variant="outline" 
                size="sm"
                className="self-start md:self-auto gap-2"
              >
                <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? "animate-spin" : ""}`} />
                {isLoading ? "Querying..." : "Execute Ping"}
              </Button>
            </div>

            {healthResult && (
              <div className="mt-4 p-4 rounded-lg bg-emerald-50/70 border border-emerald-200 font-mono text-xs text-slate-800 space-y-1">
                <div className="flex items-center gap-2 text-emerald-700 font-semibold mb-1">
                  <CheckCircle2 className="h-4 w-4" /> Endpoint responded 200 OK
                </div>
                <div>Status: <span className="font-bold text-emerald-600">{healthResult.status}</span></div>
                <div>Service: {healthResult.service}</div>
                <div>Version: {healthResult.version}</div>
                <div className="flex items-center gap-1 text-slate-500">
                  <Clock className="h-3 w-3" /> Timestamp: {healthResult.timestamp}
                </div>
              </div>
            )}

            {healthError && (
              <div className="mt-4 p-4 rounded-lg bg-amber-50/70 border border-amber-200 text-xs text-amber-900">
                <div className="font-semibold mb-1">Unable to connect to backend:</div>
                <div className="font-mono text-amber-800">{healthError}</div>
                <p className="mt-2 text-slate-600">
                  Make sure the Spring Boot application is running on port 8080. Check the backend setup instructions below.
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Architecture Highlights Grid */}
      <div id="architecture" className="space-y-6">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Phase 0 Architecture Components</h2>
          <p className="text-sm text-muted-foreground">Verified modular building blocks ready for Phase 1 feature development.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {/* Frontend */}
          <Card className="hover:shadow-md transition-shadow">
            <CardHeader className="pb-3">
              <div className="h-10 w-10 rounded-lg bg-blue-100 flex items-center justify-center text-blue-600 mb-2">
                <Layers className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Frontend Client</CardTitle>
              <CardDescription>React 18, TypeScript & Vite</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground space-y-2">
              <p>Tailwind CSS styling with shadcn/ui components design system and client-side routing with React Router.</p>
              <div className="pt-2 flex flex-wrap gap-1">
                <Badge variant="secondary">TypeScript</Badge>
                <Badge variant="secondary">Tailwind</Badge>
                <Badge variant="secondary">Vite</Badge>
              </div>
            </CardContent>
          </Card>

          {/* Backend */}
          <Card className="hover:shadow-md transition-shadow">
            <CardHeader className="pb-3">
              <div className="h-10 w-10 rounded-lg bg-emerald-100 flex items-center justify-center text-emerald-600 mb-2">
                <Server className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Backend API</CardTitle>
              <CardDescription>Spring Boot 3 & Java 21</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground space-y-2">
              <p>Production Maven structure with Spring Web, Validation, Security filter chain, and Spring Data JPA.</p>
              <div className="pt-2 flex flex-wrap gap-1">
                <Badge variant="secondary">Java 21</Badge>
                <Badge variant="secondary">Spring Web</Badge>
                <Badge variant="secondary">Maven</Badge>
              </div>
            </CardContent>
          </Card>

          {/* Database */}
          <Card className="hover:shadow-md transition-shadow">
            <CardHeader className="pb-3">
              <div className="h-10 w-10 rounded-lg bg-teal-100 flex items-center justify-center text-teal-600 mb-2">
                <Database className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Data Store</CardTitle>
              <CardDescription>Supabase PostgreSQL</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground space-y-2">
              <p>Structured for external Supabase database with connection pooling, credentials loaded securely from env vars.</p>
              <div className="pt-2 flex flex-wrap gap-1">
                <Badge variant="secondary">PostgreSQL</Badge>
                <Badge variant="secondary">Supabase</Badge>
                <Badge variant="secondary">HikariCP</Badge>
              </div>
            </CardContent>
          </Card>

          {/* Security */}
          <Card className="hover:shadow-md transition-shadow">
            <CardHeader className="pb-3">
              <div className="h-10 w-10 rounded-lg bg-indigo-100 flex items-center justify-center text-indigo-600 mb-2">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <CardTitle className="text-base">Security & CORS</CardTitle>
              <CardDescription>Zero-Trust Baseline</CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground space-y-2">
              <p>Explicit development CORS origins, public health probing, stateless session policy, secrets zero-leak guarantee.</p>
              <div className="pt-2 flex flex-wrap gap-1">
                <Badge variant="secondary">Spring Security</Badge>
                <Badge variant="secondary">CORS Filter</Badge>
                <Badge variant="secondary">Stateless</Badge>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Roadmap overview for upcoming phases */}
      <div className="mt-14 p-6 rounded-xl border border-dashed border-slate-300 bg-slate-50/50">
        <h3 className="font-semibold text-sm uppercase tracking-wider text-slate-500 mb-3 flex items-center gap-1.5">
          <Code2 className="h-4 w-4" /> Next Phase Milestones (Roadmap)
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          <div className="p-3 bg-white rounded-lg border">
            <div className="font-bold text-slate-800">Phase 1: Resume Studio & Supabase Auth</div>
            <p className="text-muted-foreground mt-1">Supabase Auth JWT integration, ATS resume parsing, and AI bullet optimization.</p>
          </div>
          <div className="p-3 bg-white rounded-lg border">
            <div className="font-bold text-slate-800">Phase 2: Career Roadmap & Gap Analysis</div>
            <p className="text-muted-foreground mt-1">Skill graph modeling, target job description comparison, learning milestones.</p>
          </div>
          <div className="p-3 bg-white rounded-lg border">
            <div className="font-bold text-slate-800">Phase 3: Application Tracker & Analytics</div>
            <p className="text-muted-foreground mt-1">Kanban application pipeline, interview schedule logs, offer negotiation assistant.</p>
          </div>
        </div>
      </div>
    </div>
  );
};
