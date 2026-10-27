import React from "react";
import { Link } from "react-router-dom";
import { Clock, ArrowLeft, CheckCircle2, Sparkles, Layers } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

interface PlaceholderPageProps {
  title: string;
  phase?: string;
  description: string;
  icon?: React.ReactNode;
  plannedFeatures?: string[];
}

export const PlaceholderPage: React.FC<PlaceholderPageProps> = ({
  title,
  phase = "Phase 3 Roadmap",
  description,
  icon,
  plannedFeatures = [],
}) => {
  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center justify-between border-b border-slate-200 pb-4">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h2>
            <Badge variant="secondary" className="text-xs">
              {phase}
            </Badge>
          </div>
          <p className="text-xs text-muted-foreground mt-1">
            Planned milestone engine for CareerPilot AI.
          </p>
        </div>

        <Link to="/dashboard">
          <Button variant="outline" size="sm" className="gap-1.5 text-xs h-9">
            <ArrowLeft className="h-3.5 w-3.5" /> Back to Dashboard
          </Button>
        </Link>
      </div>

      <Card className="border-slate-200 shadow-sm overflow-hidden">
        <div className="bg-slate-50/70 border-b border-slate-100 p-8 text-center space-y-3">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-white text-blue-700 shadow-sm border border-slate-200/80">
            {icon || <Clock className="h-7 w-7 text-blue-700" />}
          </div>
          <h3 className="text-xl font-bold text-slate-900">{title}</h3>
          <p className="text-xs sm:text-sm text-muted-foreground max-w-xl mx-auto leading-relaxed">
            {description}
          </p>
          <div className="pt-2">
            <Badge variant="outline" className="text-xs gap-1.5 bg-white font-medium">
              <Sparkles className="h-3.5 w-3.5 text-blue-600" />
              Scheduled for Phase 3 Business Implementation
            </Badge>
          </div>
        </div>

        {plannedFeatures.length > 0 && (
          <CardContent className="p-6 sm:p-8 space-y-4">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
              <Layers className="h-4 w-4" /> Planned Capabilities & Architecture
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
              {plannedFeatures.map((feat, idx) => (
                <div
                  key={idx}
                  className="flex items-start gap-2.5 p-3 rounded-lg border border-slate-100 bg-slate-50/50 text-xs text-slate-700"
                >
                  <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0 mt-0.5" />
                  <span>{feat}</span>
                </div>
              ))}
            </div>
          </CardContent>
        )}

        <CardFooter className="bg-slate-50/50 border-t border-slate-100 p-4 flex justify-between items-center text-xs text-muted-foreground">
          <span>Phase 2 Project Foundation: Shell & UI/UX Active</span>
          <Link to="/profile">
            <Button variant="ghost" size="sm" className="text-xs text-blue-700 hover:text-blue-800">
              Manage Profile &rarr;
            </Button>
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
};
