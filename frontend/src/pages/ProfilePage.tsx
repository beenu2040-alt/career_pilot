import React, { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import {
  fetchUserProfile,
  updateUserProfile,
  fetchUserSkills,
  addUserSkill,
  removeUserSkill,
  fetchCatalogSkills,
  type ProfileResponse,
  type UserSkillDto,
  type SkillDto,
} from '@/lib/api';
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
  Fingerprint,
  Plus,
  Trash2,
  Code2,
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

  // User Skills state
  const [userSkills, setUserSkills] = useState<UserSkillDto[]>([]);
  const [catalogSkills, setCatalogSkills] = useState<SkillDto[]>([]);
  const [isAddingSkill, setIsAddingSkill] = useState(false);
  const [newSkillName, setNewSkillName] = useState('');
  const [newProficiency, setNewProficiency] = useState<'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT'>('INTERMEDIATE');
  const [newYears, setNewYears] = useState('3');
  const [skillSuccess, setSkillSuccess] = useState<string | null>(null);
  const [skillError, setSkillError] = useState<string | null>(null);

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

  const loadSkills = async () => {
    if (!accessToken) return;
    try {
      const [userSkillsData, catalog] = await Promise.all([
        fetchUserSkills(accessToken),
        fetchCatalogSkills().catch(() => []),
      ]);
      setUserSkills(userSkillsData);
      setCatalogSkills(catalog);
    } catch (err) {
      console.error('Failed to load user skills:', err);
    }
  };

  useEffect(() => {
    loadProfile();
    loadSkills();
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
      
      setTimeout(() => {
        setSuccessMessage(null);
      }, 5000);
    } catch (err) {
      setErrorMessage((err as Error).message || 'Failed to update profile.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleAddSkill = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accessToken || !newSkillName.trim()) return;

    setIsAddingSkill(true);
    setSkillError(null);
    setSkillSuccess(null);

    try {
      const added = await addUserSkill(
        {
          skillName: newSkillName.trim(),
          proficiencyLevel: newProficiency,
          yearsExperience: parseInt(newYears, 10) || 0,
        },
        accessToken
      );

      // Append or replace
      setUserSkills((prev) => {
        const filtered = prev.filter((s) => s.skillId !== added.skillId);
        return [...filtered, added].sort((a, b) => a.skillName.localeCompare(b.skillName));
      });

      setNewSkillName('');
      setSkillSuccess(`Added "${added.skillName}" to your profile!`);
      setTimeout(() => setSkillSuccess(null), 4000);
    } catch (err) {
      setSkillError((err as Error).message || 'Failed to add skill.');
    } finally {
      setIsAddingSkill(false);
    }
  };

  const handleRemoveSkill = async (skillId: string) => {
    if (!accessToken) return;
    try {
      await removeUserSkill(skillId, accessToken);
      setUserSkills((prev) => prev.filter((s) => s.skillId !== skillId));
    } catch (err) {
      console.error('Failed to remove skill:', err);
    }
  };

  const getProficiencyVariant = (level: string) => {
    switch (level) {
      case 'EXPERT':
        return 'success';
      case 'ADVANCED':
        return 'info';
      case 'INTERMEDIATE':
        return 'secondary';
      default:
        return 'outline';
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Banner & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-slate-900">Career Profile</h2>
          <p className="text-xs text-muted-foreground mt-1">
            Manage your personal identifiers and verified technical skills to power precision job matching.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              loadProfile();
              loadSkills();
            }}
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

        {/* Right 2 Columns: Edit Profile Form + Skills Inventory */}
        <div className="lg:col-span-2 space-y-8">
          {/* Form Card */}
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

          {/* Technical Skills Inventory Card */}
          <Card className="border-slate-200 shadow-sm">
            <CardHeader className="border-b border-slate-100 pb-4">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-lg text-slate-900 flex items-center gap-2">
                    <Code2 className="h-5 w-5 text-blue-700" />
                    Technical Skills Inventory
                  </CardTitle>
                  <CardDescription>
                    Your verified competencies. Used by the explainable matching engine to calculate deterministic match scores.
                  </CardDescription>
                </div>
                <Badge variant="secondary" className="text-xs">
                  {userSkills.length} Cataloged
                </Badge>
              </div>
            </CardHeader>

            <CardContent className="pt-6 space-y-6">
              {skillSuccess && (
                <div className="rounded-lg bg-emerald-50 p-3 text-xs text-emerald-800 border border-emerald-200 flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                  <span>{skillSuccess}</span>
                </div>
              )}

              {skillError && (
                <div className="rounded-lg bg-rose-50 p-3 text-xs text-rose-800 border border-rose-200 flex items-center gap-2">
                  <AlertCircle className="h-4 w-4 text-rose-600 shrink-0" />
                  <span>{skillError}</span>
                </div>
              )}

              {/* Add Skill Form */}
              <form onSubmit={handleAddSkill} className="rounded-xl border border-slate-100 bg-slate-50/70 p-4 space-y-3">
                <span className="text-xs font-semibold text-slate-700 block">Add New Competency</span>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  {/* Skill Name */}
                  <div className="space-y-1">
                    <label className="text-[11px] font-medium text-slate-600">Skill Name</label>
                    <input
                      type="text"
                      list="catalog-skills-list"
                      placeholder="e.g. Java, React, Docker..."
                      value={newSkillName}
                      onChange={(e) => setNewSkillName(e.target.value)}
                      required
                      className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-xs shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-600"
                    />
                    <datalist id="catalog-skills-list">
                      {catalogSkills.map((s) => (
                        <option key={s.id} value={s.name} />
                      ))}
                    </datalist>
                  </div>

                  {/* Proficiency Level */}
                  <div className="space-y-1">
                    <label className="text-[11px] font-medium text-slate-600">Proficiency</label>
                    <select
                      value={newProficiency}
                      onChange={(e) => setNewProficiency(e.target.value as any)}
                      className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-xs shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-600"
                    >
                      <option value="BEGINNER">Beginner</option>
                      <option value="INTERMEDIATE">Intermediate</option>
                      <option value="ADVANCED">Advanced</option>
                      <option value="EXPERT">Expert</option>
                    </select>
                  </div>

                  {/* Years Experience */}
                  <div className="space-y-1">
                    <label className="text-[11px] font-medium text-slate-600">Years Experience</label>
                    <input
                      type="number"
                      min="0"
                      max="50"
                      value={newYears}
                      onChange={(e) => setNewYears(e.target.value)}
                      className="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-xs shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-600"
                    />
                  </div>
                </div>

                <div className="flex justify-end pt-1">
                  <Button
                    type="submit"
                    size="sm"
                    disabled={isAddingSkill || !newSkillName.trim()}
                    className="text-xs h-9 bg-blue-700 hover:bg-blue-800 text-white gap-1.5 shadow-sm"
                  >
                    {isAddingSkill ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Plus className="h-3.5 w-3.5" />}
                    Add Competency
                  </Button>
                </div>
              </form>

              {/* Skills List */}
              <div className="space-y-3">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 block">
                  Cataloged Skills ({userSkills.length})
                </span>

                {userSkills.length > 0 ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {userSkills.map((us) => (
                      <div
                        key={us.skillId}
                        className="flex items-center justify-between p-3 rounded-lg border border-slate-200 bg-white hover:border-slate-300 transition-colors"
                      >
                        <div className="space-y-0.5 min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-xs text-slate-900 truncate">{us.skillName}</span>
                            <Badge variant={getProficiencyVariant(us.proficiencyLevel)} className="text-[10px] py-0">
                              {us.proficiencyLevel}
                            </Badge>
                          </div>
                          <p className="text-[11px] text-muted-foreground">
                            {us.category || "General"} • {us.yearsExperience} yr{us.yearsExperience === 1 ? "" : "s"}
                          </p>
                        </div>

                        <button
                          onClick={() => handleRemoveSkill(us.skillId)}
                          className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-md transition-colors"
                          title="Remove skill"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="rounded-lg border border-dashed border-slate-200 p-6 text-center text-xs text-muted-foreground">
                    <p>No skills cataloged yet. Add your core technical competencies above to power job matching.</p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};
