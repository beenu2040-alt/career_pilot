import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { Compass, AlertCircle, Loader2, ArrowRight, Mail, Lock } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { signIn, isConfigured } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/dashboard';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      const { error: signInError } = await signIn(email, password);
      if (signInError) {
        setError(signInError.message);
      } else {
        navigate(from, { replace: true });
      }
    } catch (err) {
      setError((err as Error).message || 'An unexpected error occurred during login');
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
              <Compass className="h-6 w-6" />
            </div>
          </div>
          <CardTitle className="text-2xl font-bold tracking-tight text-slate-900">Sign in to CareerPilot</CardTitle>
          <CardDescription className="text-xs">
            Enter your credentials to access your personalized career hub.
          </CardDescription>
        </CardHeader>

        <CardContent>
          {!isConfigured && (
            <div className="mb-4 rounded-lg bg-amber-50 p-3 text-xs text-amber-800 border border-amber-200 flex items-start gap-2">
              <AlertCircle className="h-4 w-4 text-amber-600 mt-0.5 shrink-0" />
              <div>
                <span className="font-semibold">Supabase Keys Not Configured</span>: Configure{' '}
                <code className="bg-amber-100 px-1 py-0.5 rounded">VITE_SUPABASE_URL</code> and{' '}
                <code className="bg-amber-100 px-1 py-0.5 rounded">VITE_SUPABASE_ANON_KEY</code> in{' '}
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

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Email Address"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="name@example.com"
              leftIcon={<Mail className="h-4 w-4" />}
            />

            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700">
                  Password
                </label>
                <Link to="/forgot-password" className="text-xs text-blue-700 hover:underline">
                  Forgot password?
                </Link>
              </div>
              <Input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                leftIcon={<Lock className="h-4 w-4" />}
              />
            </div>

            <Button
              type="submit"
              disabled={isLoading}
              className="w-full gap-2 mt-2 h-10 bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs shadow-sm"
            >
              {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              {isLoading ? 'Signing in...' : 'Sign In'}
            </Button>
          </form>
        </CardContent>

        <CardFooter className="flex flex-col space-y-2 border-t border-slate-100 pt-4 text-center text-xs text-muted-foreground">
          <div>
            Don't have an account?{' '}
            <Link to="/register" className="font-semibold text-blue-700 hover:underline inline-flex items-center gap-0.5">
              Sign up <ArrowRight className="h-3 w-3" />
            </Link>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
};
