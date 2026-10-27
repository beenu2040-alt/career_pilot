import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { KeyRound, AlertCircle, CheckCircle2, Loader2, ArrowLeft, Mail } from 'lucide-react';

export const ForgotPasswordPage: React.FC = () => {
  const { resetPasswordForEmail, isConfigured } = useAuth();
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      const { error: resetError } = await resetPasswordForEmail(email);
      if (resetError) {
        setError(resetError.message);
      } else {
        setIsSuccess(true);
      }
    } catch (err) {
      setError((err as Error).message || 'An error occurred while sending the reset email.');
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
              <KeyRound className="h-6 w-6" />
            </div>
          </div>
          <CardTitle className="text-2xl font-bold tracking-tight text-slate-900">Reset your password</CardTitle>
          <CardDescription className="text-xs">
            Enter your account email and we'll send you a link to reset your password.
          </CardDescription>
        </CardHeader>

        <CardContent>
          {!isConfigured && (
            <div className="mb-4 rounded-lg bg-amber-50 p-3 text-xs text-amber-800 border border-amber-200 flex items-start gap-2">
              <AlertCircle className="h-4 w-4 text-amber-600 mt-0.5 shrink-0" />
              <span>Supabase credentials are not configured in <code className="bg-amber-100 px-1 rounded">frontend/.env</code>.</span>
            </div>
          )}

          {error && (
            <div className="mb-4 rounded-lg bg-rose-50 p-3 text-xs text-rose-800 border border-rose-200 flex items-start gap-2">
              <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {isSuccess ? (
            <div className="rounded-lg bg-emerald-50 p-4 text-sm text-emerald-800 border border-emerald-200 space-y-2 text-center">
              <CheckCircle2 className="h-6 w-6 text-emerald-600 mx-auto" />
              <div className="font-semibold text-slate-900">Reset Link Sent</div>
              <p className="text-xs text-slate-600">
                If an account exists for <span className="font-semibold text-slate-800">{email}</span>, you will receive password reset instructions shortly.
              </p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                label="Account Email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@example.com"
                leftIcon={<Mail className="h-4 w-4" />}
              />

              <Button
                type="submit"
                disabled={isLoading}
                className="w-full gap-2 mt-2 h-10 bg-blue-700 hover:bg-blue-800 text-white font-semibold text-xs shadow-sm"
              >
                {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                {isLoading ? 'Sending reset link...' : 'Send Reset Link'}
              </Button>
            </form>
          )}
        </CardContent>

        <CardFooter className="flex justify-center border-t border-slate-100 pt-4 text-xs text-muted-foreground">
          <Link to="/login" className="font-semibold text-blue-700 hover:underline flex items-center gap-1">
            <ArrowLeft className="h-3 w-3" /> Back to sign in
          </Link>
        </CardFooter>
      </Card>
    </div>
  );
};
