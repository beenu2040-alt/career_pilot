import React, { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { fetchUserProfile, updateUserProfile, type ProfileResponse } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { 
  User as UserIcon, 
  ShieldCheck, 
  Database, 
  CheckCircle2, 
  AlertCircle, 
  Loader2, 
  Sparkles,
  Save,
  Clock,
  MapPin,
  Briefcase
} from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { user, accessToken, signOut } = useAuth();

  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [fullName, setFullName] = useState('');
  const [headline, setHeadline] = useState('');
  const [location, setLocation] = useState('');
  const [targetRole, setTargetRole] = useState('');

  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isNewProfile, setIsNewProfile] = useState(false);

  useEffect(() => {
    let isMounted = true;

    const loadProfile = async () => {
      if (!accessToken) {
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setErrorMessage(null);

      try {
        const data = await fetchUserProfile(accessToken);
        if (isMounted) {
          if (data) {
            setProfile(data);
            setFullName(data.fullName || '');
            setHeadline(data.headline || '');
            setLocation(data.location || '');
            setTargetRole(data.targetRole || '');
            setIsNewProfile(false);
          } else {
            setIsNewProfile(true);
            // Default full name from auth metadata if available
            setFullName(user?.user_metadata?.full_name || '');
          }
        }
      } catch (err) {
        if (isMounted) {
          setErrorMessage((err as Error).message || 'Failed to connect to backend profile service.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    loadProfile();

    return () => {
      isMounted = false;
    };
  }, [accessToken, user]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accessToken) {
      setErrorMessage('Authentication session expired. Please sign in again.');
      return;
    }

    setIsSaving(true);
    setSuccessMessage(null);
    setErrorMessage(null);

    try {
      const updated = await updateUserProfile(accessToken, {
        fullName,
        headline,
        location,
        targetRole,
      });

      setProfile(updated);
      setIsNewProfile(false);
      setSuccessMessage('Profile successfully saved to Supabase PostgreSQL!');
    } catch (err) {
      setErrorMessage((err as Error).message || 'Failed to update profile.');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto flex min-h-[60vh] items-center justify-center px-4">
        <div className="flex flex-col items-center gap-2 text-muted-foreground">
          <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
          <p className="text-sm">Loading profile from backend...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-5xl px-4 py-10 sm:px-8">
      {/* Header Info */}
      <div className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4 border-b pb-6">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-3xl font-bold tracking-tight text-slate-900">Career Profile</h1>
            <Badge variant="success" className="gap-1 text-xs">
              <ShieldCheck className="h-3.5 w-3.5 text-emerald-600" /> Authenticated
            </Badge>
          </div>
          <p className="text-sm text-muted-foreground mt-1">
            Manage your personal profile and career target preferences. Secured via Supabase Auth & Spring Security.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button variant="outline" size="sm" onClick={() => signOut()}>
            Sign Out
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Identity & Security Card */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center gap-2">
                <UserIcon className="h-4 w-4 text-blue-600" /> Authenticated Identity
              </CardTitle>
              <CardDescription>Verified Supabase Claims</CardDescription>
            </CardHeader>
            <CardContent className="text-xs space-y-3 font-mono">
              <div>
                <span className="text-slate-400 block text-[10px] uppercase">Supabase User UUID</span>
                <span className="text-slate-800 break-all select-all font-semibold">{user?.id}</span>
              </div>
              <div>
                <span className="text-slate-400 block text-[10px] uppercase">Registered Email</span>
                <span className="text-slate-800 break-all">{user?.email}</span>
              </div>
              <div>
                <span className="text-slate-400 block text-[10px] uppercase">Email Confirmed</span>
                <span className={user?.confirmed_at ? "text-emerald-600 font-bold" : "text-amber-600"}>
                  {user?.confirmed_at ? "Confirmed" : "Pending Confirmation"}
                </span>
              </div>
              <div className="pt-2 border-t text-[11px] font-sans text-muted-foreground space-y-1.5">
                <div className="flex items-center gap-1.5 text-emerald-700">
                  <CheckCircle2 className="h-3.5 w-3.5 shrink-0" />
                  <span>Spring Security JWT Signature Validated</span>
                </div>
                <div className="flex items-center gap-1.5 text-blue-700">
                  <Database className="h-3.5 w-3.5 shrink-0" />
                  <span>Enforces User Ownership in JPA Queries</span>
                </div>
              </div>
            </CardContent>
          </Card>

          {profile && (
            <Card className="bg-slate-50/50">
              <CardHeader className="pb-2">
                <CardTitle className="text-xs font-semibold uppercase tracking-wider text-slate-500 flex items-center gap-1">
                  <Clock className="h-3.5 w-3.5" /> Metadata
                </CardTitle>
              </CardHeader>
              <CardContent className="text-xs text-muted-foreground space-y-1">
                <div>Created: {new Date(profile.createdAt).toLocaleString()}</div>
                <div>Updated: {new Date(profile.updatedAt).toLocaleString()}</div>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Right Column: Edit Profile Form */}
        <div className="lg:col-span-2 space-y-6">
          <Card className="shadow-sm">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-xl">Profile Details</CardTitle>
                  <CardDescription>
                    Information utilized across the Resume Studio and Career Roadmap.
                  </CardDescription>
                </div>
                {isNewProfile && (
                  <Badge variant="secondary" className="gap-1 text-xs">
                    <Sparkles className="h-3 w-3 text-blue-500" /> New Profile
                  </Badge>
                )}
              </div>
            </CardHeader>

            <CardContent>
              {successMessage && (
                <div className="mb-4 rounded-lg bg-emerald-50 p-3 text-xs text-emerald-800 border border-emerald-200 flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600 mt-0.5 shrink-0" />
                  <span>{successMessage}</span>
                </div>
              )}

              {errorMessage && (
                <div className="mb-4 rounded-lg bg-destructive/10 p-3 text-xs text-destructive border border-destructive/20 flex items-start gap-2">
                  <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                  <span>{errorMessage}</span>
                </div>
              )}

              {isNewProfile && !successMessage && (
                <div className="mb-6 rounded-lg bg-blue-50 p-3 text-xs text-blue-800 border border-blue-200">
                  <p className="font-semibold mb-0.5">Welcome to CareerPilot AI!</p>
                  <p>You haven't set up your profile yet. Fill in your details below to initialize your career record.</p>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                    Full Name
                  </label>
                  <input
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="e.g. Alex Rivera"
                    maxLength={255}
                    className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors focus:border-blue-600 focus:outline-none focus:ring-1 focus:ring-blue-600"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold uppercase tracking-wider text-slate-700 flex items-center gap-1">
                    <Briefcase className="h-3 w-3 text-slate-400" /> Professional Headline
                  </label>
                  <input
                    type="text"
                    value={headline}
                    onChange={(e) => setHeadline(e.target.value)}
                    placeholder="e.g. Senior Backend Engineer specializing in Java & Distributed Systems"
                    maxLength={255}
                    className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors focus:border-blue-600 focus:outline-none focus:ring-1 focus:ring-blue-600"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="text-xs font-semibold uppercase tracking-wider text-slate-700 flex items-center gap-1">
                      <MapPin className="h-3 w-3 text-slate-400" /> Location
                    </label>
                    <input
                      type="text"
                      value={location}
                      onChange={(e) => setLocation(e.target.value)}
                      placeholder="e.g. San Francisco, CA / Remote"
                      maxLength={255}
                      className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors focus:border-blue-600 focus:outline-none focus:ring-1 focus:ring-blue-600"
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                      Target Role
                    </label>
                    <input
                      type="text"
                      value={targetRole}
                      onChange={(e) => setTargetRole(e.target.value)}
                      placeholder="e.g. Principal Architect / Engineering Lead"
                      maxLength={255}
                      className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors focus:border-blue-600 focus:outline-none focus:ring-1 focus:ring-blue-600"
                    />
                  </div>
                </div>

                <div className="pt-4 flex justify-end">
                  <Button type="submit" disabled={isSaving} className="gap-2">
                    {isSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                    {isSaving ? 'Saving Profile...' : 'Save Profile'}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};
