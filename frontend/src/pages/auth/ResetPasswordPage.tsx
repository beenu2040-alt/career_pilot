import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { ShieldCheck, AlertCircle, CheckCircle2, Loader2, ArrowRight, Lock } from 'lucide-react';

export const ResetPasswordPage: React.FC = () => {
  const { updatePassword } = useAuth();
  const navigate = useNavigate();

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setIsLoading(true);

    try {
      const { error: updateError } = await updatePassword(newPassword);
      if (updateError) {
        setError(updateError.message);
      } else {
        setIsSuccess(true);
        setTimeout(() => {
          navigate('/dashboard');
        }, 2000);
      }
    } catch (err) {
      setError((err as Error).message || 'Failed to update password.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="container mx-auto flex min-h-[75vh] max-w-md items-center justify-center px-4 py-12">
      <Card className="w-full shadow-md border-slate-200">
        <CardHeader className="space-y-1.5 text-center">
          <div className="flex justify-center mb-2">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-700 text-white shadow-sm">
              <ShieldCheck className="h-6 w-6" />
            </div>
          </div>
          <CardTitle className="text-2xl font-bold tracking-tight text-slate-900">Set New Password</CardTitle>
          <CardDescription className="text-xs">
            Enter your new secure password below.
          </CardDescription>
        </CardHeader>

        <CardContent>
          {error && (
            <div className="mb-4 rounded-lg bg-rose-50 p-3 text-xs text-rose-800 border border-rose-200 flex items-start gap-2">
              <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
              <span>{error}</span>
            </div>
          )}

          {isSuccess ? (
            <div className="rounded-lg bg-emerald-50 p-4 text-sm text-emerald-800 border border-emerald-200 space-y-2 text-center">
              <CheckCircle2 className="h-6 w-6 text-emerald-600 mx-auto" />
              <div className="font-semibold text-slate-900">Password Updated Successfully</div>
              <p className="text-xs text-slate-600">
                Redirecting you to your dashboard...
              </p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                label="New Password"
                type="password"
                required
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="At least 6 characters"
                leftIcon={<Lock className="h-4 w-4" />}
              />

              <Input
                label="Confirm New Password"
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Re-enter new password"
                leftIcon={<Lock className="h-4 w-4" />}
              />

              <Button
                type="submit"
                disabled={isLoading}
                className="w-full gap-2 mt-2 h-10 bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs shadow-sm"
              >
                {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                {isLoading ? 'Updating password...' : 'Update Password'}
              </Button>
            </form>
          )}
        </CardContent>

        <CardFooter className="flex justify-center border-t border-slate-100 pt-4 text-xs text-muted-foreground">
          <Link to="/login" className="font-semibold text-blue-700 hover:underline flex items-center gap-1">
            Sign In with current password <ArrowRight className="h-3 w-3" />
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
};
