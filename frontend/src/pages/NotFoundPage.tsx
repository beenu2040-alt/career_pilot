import React from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Compass } from "lucide-react";
import { Button } from "@/components/ui/button";

export const NotFoundPage: React.FC = () => {
  return (
    <div className="container mx-auto max-w-2xl px-4 py-24 text-center space-y-4">
      <div className="flex justify-center mb-2">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-50 text-blue-700">
          <Compass className="h-6 w-6" />
        </div>
      </div>
      <h1 className="text-6xl font-black text-slate-900 tracking-tight">404</h1>
      <h2 className="text-xl font-bold text-slate-800">Page Not Found</h2>
      <p className="text-xs text-muted-foreground max-w-sm mx-auto">
        The route you navigated to does not exist or has been relocated within the workspace.
      </p>
      <div className="pt-4 flex items-center justify-center gap-3">
        <Link to="/dashboard">
          <Button className="gap-2 bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs">
            <ArrowLeft className="h-4 w-4" /> Go to Dashboard
          </Button>
        </Link>
        <Link to="/">
          <Button variant="outline" className="text-xs border-slate-200">
            Home Page
          </Button>
        </Link>
      </div>
    </div>
  );
};
