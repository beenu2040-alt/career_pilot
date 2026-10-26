import React from "react";
import { Link } from "react-router-dom";
import { Clock, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

interface PlaceholderPageProps {
  title: string;
  phase: string;
  description: string;
}

export const PlaceholderPage: React.FC<PlaceholderPageProps> = ({ title, phase, description }) => {
  return (
    <div className="container mx-auto max-w-4xl px-4 py-16 sm:px-8">
      <Card className="border-dashed text-center p-8">
        <CardContent className="flex flex-col items-center space-y-4">
          <div className="h-12 w-12 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center">
            <Clock className="h-6 w-6" />
          </div>
          <Badge variant="outline">{phase}</Badge>
          <h2 className="text-2xl font-bold text-slate-900">{title}</h2>
          <p className="text-sm text-muted-foreground max-w-md">
            {description}
          </p>
          <p className="text-xs text-slate-400">
            Per Phase 0 constraints, business features remain un-implemented until the foundation is verified.
          </p>
          <div className="pt-4">
            <Link to="/">
              <Button variant="outline" size="sm" className="gap-2">
                <ArrowLeft className="h-4 w-4" /> Return to Overview
              </Button>
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
