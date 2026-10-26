import React from "react";
import { Compass, Shield, Database, Cpu, Layers } from "lucide-react";

export const Footer: React.FC = () => {
  return (
    <footer className="border-t border-border/40 bg-muted/30 py-8 text-sm text-muted-foreground">
      <div className="container mx-auto max-w-7xl px-4 sm:px-8 flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-2">
          <Compass className="h-4 w-4 text-blue-600" />
          <span className="font-semibold text-foreground">CareerPilot AI</span>
          <span>— Production Architecture Foundation</span>
        </div>
        <div className="flex flex-wrap items-center gap-4 text-xs">
          <span className="flex items-center gap-1">
            <Layers className="h-3.5 w-3.5 text-blue-500" /> React 18 + Vite
          </span>
          <span className="flex items-center gap-1">
            <Cpu className="h-3.5 w-3.5 text-emerald-500" /> Spring Boot 3 + Java 21
          </span>
          <span className="flex items-center gap-1">
            <Database className="h-3.5 w-3.5 text-teal-500" /> Supabase PostgreSQL
          </span>
          <span className="flex items-center gap-1">
            <Shield className="h-3.5 w-3.5 text-indigo-500" /> Spring Security
          </span>
        </div>
      </div>
    </footer>
  );
};
