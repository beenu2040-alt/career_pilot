import React, { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { fetchUserProfile, updateUserProfile, type ProfileResponse } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
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
  Briefcase,
  RefreshCw,
  Mail,
  Fingerprint
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
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [isNewProfile, setIsNewProfile] = useState(false);

  const loadProfile = async () => {
    if (!accessToken) {
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    try {
      const data = await fetchUserProfile(accessToken);
      if (data) {
        setProfile(data);
        setFullName(data.fullName || '');
        setHeadline(data.headline || '');
        setLocation(data.location || '');
        setTargetRole(data.targetRole || '');
        setIsNewProfile(false);
      } else {
        setIsNewProfile(true);
        setFullName(user?.user_metadata?.full_name || '');
      }
    } catch (err) {
      setErrorMessage((err as Error).message || 'Failed to connect to backend profile service.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, [accessToken, user]);

  const validate = (): boolean => {
    const errors: Record<string, string> = {};
    if (fullName.length > 255) {
      errors.fullName = 'Full name must not exceed 255 characters';
    }
    if (headline.length > 255) {
      errors.headline = 'Headline must not exceed 255 characters';
    }
    if (location.length > 255) {
      errors.location = 'Location must not exceed 255 characters';
    }
    if (targetRole.length > 255) {
      errors.targetRole = 'Target role must not exceed 255 characters';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accessToken) {
      setErrorMessage('Authentication session expired. Please sign in again.');
      return;
    }

    if (!validate()) {
      return;
    }

    setIsSaving(true);
    setSuccessMessage(null);
    setErrorMessage(null);

    try {
      const updated = await updateUserProfile(accessToken, {
        fullName: fullName.trim(),
        headline: headline.trim(),
        location: location.trim(),
        targetRole: targetRole.trim(),
      });

      setProfile(updated);
      setIsNewProfile(false);
      setSuccessMessage('Profile saved successfully and synchronized with Supabase PostgreSQL.');
      
      // Auto-dismiss success notification after 5 seconds
      setTimeout(() => {
        setSuccessMessage(null);
      }, 5000);
    } catch (err) {
      setErrorMessage((err as Error).message || 'Failed to update profile.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Banner & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-slate-900">Career Profile</h2>
          <p className="text-xs text-muted-foreground mt-1">
            Manage your personal identifiers and target position to power precision career recommendations.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={loadProfile}
            disabled={isLoading || isSaving}
            className="text-xs h-9 gap-1.5"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? "animate-spin" : ""}`} />
            Refresh Data
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => signOut()}
            className="text-xs h-9 text-slate-600 hover:text-rose-600 hover:border-rose-200"
          >
            Sign Out
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Supabase Identity & Metadata Card */}
        <div className="space-y-6">
          <Card className="border-slate-200 shadow-sm">
            <CardHeader className="pb-3 border-b border-slate-100">
              <CardTitle className="text-sm font-semibold flex items-center gap-2 text-slate-900">
                <ShieldCheck className="h-4 w-4 text-blue-700" />
                Verified Identity
              </CardTitle>
              <CardDescription>
                Cryptographic authentication claims from Supabase Auth.
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-4 space-y-3.5 text-xs font-mono">
              <div>
                <span className="text-[10px] text-slate-400 block uppercase font-semibold">User UUID (Subject)</span>
                <span className="text-slate-800 break-all select-all font-medium">{user?.id}</span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 block uppercase font-semibold">Email</span>
                <span className="text-slate-800 break-all flex items-center gap-1.5 font-sans font-medium">
                  <Mail className="h-3.5 w-3.5 text-slate-400" /> {user?.email}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 block uppercase font-semibold">Email Verification</span>
                <span className="flex items-center gap-1 font-sans">
                  {user?.confirmed_at ? (
                    <Badge variant="success" className="text-[10px]">Verified</Badge>
                  ) : (
                    <Badge variant="warning" className="text-[10px]">Pending Confirmation</Badge>
                  )}
                </span>
              </div>
              <div className="pt-2 border-t border-slate-100 space-y-2 font-sans text-[11px] text-muted-foreground">
                <div className="flex items-center gap-2 text-emerald-700">
                  <CheckCircle2 className="h-3.5 w-3.5 shrink-0" />
                  <span>Enforced at API Gateway (OAuth2 JWT)</span>
                </div>
                <div className="flex items-center gap-2 text-blue-700">
                  <Database className="h-3.5 w-3.5 shrink-0" />
                  <span>PostgreSQL Ownership Validated</span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Metadata Card */}
          {profile && (
            <Card className="border-slate-200 bg-slate-50/50">
              <CardHeader className="pb-2">
                <CardTitle className="text-xs font-semibold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5" /> Record Timestamps
                </CardTitle>
              </CardHeader>
              <CardContent className="text-xs text-muted-foreground space-y-1">
                <div>Created: {new Date(profile.createdAt).toLocaleString()}</div>
                <div>Updated: {new Date(profile.updatedAt).toLocaleString()}</div>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Right 2 Columns: Edit Profile Form */}
        <div className="lg:col-span-2 space-y-6">
          <Card className="border-slate-200 shadow-sm">
            <CardHeader className="border-b border-slate-100 pb-4">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-lg text-slate-900">Personal & Target Role Details</CardTitle>
                  <CardDescription>
                    Information used across CareerPilot to benchmark your competencies against market demand.
                  </CardDescription>
                </div>
                {isNewProfile && (
                  <Badge variant="secondary" className="gap-1 text-xs">
                    <Sparkles className="h-3 w-3 text-blue-600" /> New Profile
                  </Badge>
                )}
              </div>
            </CardHeader>

            <CardContent className="pt-6">
              {/* Notifications */}
              {successMessage && (
                <div className="mb-6 rounded-lg bg-emerald-50 p-3.5 text-xs text-emerald-800 border border-emerald-200 flex items-start gap-2 animate-in fade-in">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600 mt-0.5 shrink-0" />
                  <span>{successMessage}</span>
                </div>
              )}

              {errorMessage && (
                <div className="mb-6 rounded-lg bg-rose-50 p-3.5 text-xs text-rose-800 border border-rose-200 flex items-start gap-2">
                  <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
                  <span>{errorMessage}</span>
                </div>
              )}

              {isNewProfile && !successMessage && (
                <div className="mb-6 rounded-lg bg-blue-50/70 p-3.5 text-xs text-blue-900 border border-blue-200">
                  <p className="font-semibold mb-0.5">Welcome! Let's set up your profile</p>
                  <p className="text-slate-600">Enter your details below to initialize your career record in the database.</p>
                </div>
              )}

              {isLoading ? (
                <div className="space-y-5">
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-24" />
                    <Skeleton className="h-10 w-full" />
                  </div>
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-32" />
                    <Skeleton className="h-10 w-full" />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Skeleton className="h-4 w-20" />
                      <Skeleton className="h-10 w-full" />
                    </div>
                    <div className="space-y-2">
                      <Skeleton className="h-4 w-24" />
                      <Skeleton className="h-10 w-full" />
                    </div>
                  </div>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="space-y-5">
                  <Input
                    label="Full Name"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="e.g. Jane Doe"
                    error={validationErrors.fullName}
                    helperText="Your preferred name on candidate materials"
                    leftIcon={<UserIcon className="h-4 w-4" />}
                    maxLength={255}
                  />

                  <Input
                    label="Professional Headline"
                    value={headline}
                    onChange={(e) => setHeadline(e.target.value)}
                    placeholder="e.g. Senior Backend Engineer specializing in Distributed Systems & Java"
                    error={validationErrors.headline}
                    helperText="A concise summary of your current expertise"
                    leftIcon={<Briefcase className="h-4 w-4" />}
                    maxLength={255}
                  />

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                    <Input
                      label="Location"
                      value={location}
                      onChange={(e) => setLocation(e.target.value)}
                      placeholder="e.g. San Francisco, CA (or Remote)"
                      error={validationErrors.location}
                      leftIcon={<MapPin className="h-4 w-4" />}
                      maxLength={255}
                    />

                    <Input
                      label="Target Role"
                      value={targetRole}
                      onChange={(e) => setTargetRole(e.target.value)}
                      placeholder="e.g. Principal Platform Architect"
                      error={validationErrors.targetRole}
                      helperText="The career position you are working toward"
                      leftIcon={<Fingerprint className="h-4 w-4" />}
                      maxLength={255}
                    />
                  </div>

                  <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-3">
                    <Button
                      type="submit"
                      disabled={isSaving}
                      className="gap-2 bg-blue-700 hover:bg-blue-800 text-white shadow-sm h-10 px-5 text-xs font-semibold"
                    >
                      {isSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                      {isSaving ? "Saving to Database..." : "Save Profile"}
                    </Button>
                  </div>
                </form>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};
