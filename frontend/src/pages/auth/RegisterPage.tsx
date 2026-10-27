import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { Compass, AlertCircle, CheckCircle2, Loader2, ArrowLeft, ArrowRight, User, Mail, Lock } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const { signUp, isConfigured } = useAuth();
  const navigate = useNavigate();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isVerificationSent, setIsVerificationSent] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setIsLoading(true);

    try {
      const { user, error: signUpError } = await signUp(email, password, fullName);
      if (signUpError) {
        setError(signUpError.message);
      } else if (user && user.identities && user.identities.length === 0) {
        setError('An account with this email address already exists.');
      } else {
        // If Supabase has email confirmation enabled, user.session will be null
        setIsVerificationSent(true);
        // If user already has an active session, navigate directly
        if (user && user.confirmed_at) {
          navigate('/dashboard', { replace: true });
        }
      }
    } catch (err) {
      setError((err as Error).message || 'An unexpected error occurred during registration.');
    } finally {
      setIsLoading(false);
    }
  };

  if (isVerificationSent) {
    return (
      <div className="container mx-auto flex min-h-[75vh] max-w-md items-center justify-center px-4 py-12">
        <Card className="w-full shadow-md border-slate-200 text-center">
          <CardHeader className="space-y-2">
            <div className="flex justify-center mb-1">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100 text-emerald-600">
                <CheckCircle2 className="h-6 w-6" />
              </div>
            </div>
            <CardTitle className="text-2xl font-bold text-slate-900">Registration Successful</CardTitle>
            <CardDescription className="text-xs">
              We've registered your account for <span className="font-semibold text-slate-800">{email}</span>.
            </CardDescription>
          </CardHeader>
          <CardContent className="text-xs text-muted-foreground space-y-3">
            <p>
              Depending on your Supabase project settings, a verification email may have been sent.
              Please check your inbox to confirm your email before signing in.
            </p>
          </CardContent>
          <CardFooter className="flex justify-center pt-2">
            <Link to="/login">
              <Button className="gap-2 bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs">
                <ArrowLeft className="h-4 w-4" /> Go to Sign In
              </Button>
            </Link>
          </CardFooter>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto flex min-h-[75vh] max-w-md items-center justify-center px-4 py-12">
      <Card className="w-full shadow-md border-slate-200">
        <CardHeader className="space-y-1.5 text-center">
          <div className="flex justify-center mb-2">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-700 text-white shadow-sm">
              <Compass className="h-6 w-6" />
            </div>
          </div>
          <CardTitle className="text-2xl font-bold tracking-tight text-slate-900">Create your account</CardTitle>
          <CardDescription className="text-xs">
            Join CareerPilot AI to accelerate your career trajectory.
          </CardDescription>
        </CardHeader>

        <CardContent>
          {!isConfigured && (
            <div className="mb-4 rounded-lg bg-amber-50 p-3 text-xs text-amber-800 border border-amber-200 flex items-start gap-2">
              <AlertCircle className="h-4 w-4 text-amber-600 mt-0.5 shrink-0" />
              <div>
                <span className="font-semibold">Supabase Not Configured</span>: Configure your Supabase project keys in{' '}
                <code className="bg-amber-100 px-1 py-0.5 rounded">frontend/.env</code>.
              </div>
            </div>
          )}

          {error && (
            <div className="mb-4 rounded-lg bg-rose-50 p-3 text-xs text-rose-800 border border-rose-200 flex items-start gap-2">
              <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-3.5">
            <Input
              label="Full Name"
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Alex Morgan"
              leftIcon={<User className="h-4 w-4" />}
            />

            <Input
              label="Email Address"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="alex@example.com"
              leftIcon={<Mail className="h-4 w-4" />}
            />

            <Input
              label="Password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="At least 6 characters"
              leftIcon={<Lock className="h-4 w-4" />}
            />

            <Input
              label="Confirm Password"
              type="password"
              required
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Re-enter password"
              leftIcon={<Lock className="h-4 w-4" />}
            />

            <Button
              type="submit"
              disabled={isLoading}
              className="w-full gap-2 mt-2 h-10 bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs shadow-sm"
            >
              {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              {isLoading ? 'Creating account...' : 'Create Account'}
            </Button>
          </form>
        </CardContent>

        <CardFooter className="flex flex-col space-y-2 border-t border-slate-100 pt-4 text-center text-xs text-muted-foreground">
          <div>
            Already have an account?{' '}
            <Link to="/login" className="font-semibold text-blue-700 hover:underline inline-flex items-center gap-0.5">
              Sign in <ArrowRight className="h-3 w-3" />
            </Link>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
};
