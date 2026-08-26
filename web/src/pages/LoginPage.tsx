import { FormEvent, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { ApiError } from '../lib/api';
import { useAuth } from '../auth/AuthContext';

export default function LoginPage() {
  const { user, isLoading, login, register } = useAuth();
  const navigate = useNavigate();
  const [isRegistering, setIsRegistering] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (isLoading) {
    return null;
  }
  if (user) {
    return <Navigate to="/" replace />;
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    if (isRegistering && password.length < 12) {
      setError('Password must be at least 12 characters.');
      return;
    }
    setIsSubmitting(true);
    try {
      if (isRegistering) {
        await register({ email, password, displayName });
      } else {
        await login({ email, password });
      }
      navigate('/', { replace: true });
    } catch (submissionError) {
      setError(
        submissionError instanceof ApiError ? submissionError.message : 'Unable to sign you in.',
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="mx-auto mt-16 max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-semibold">
        {isRegistering ? 'Create your account' : 'Sign in'}
      </h1>
      <p className="mt-2 text-slate-600">
        {isRegistering ? 'Start tracking your expenses.' : 'Welcome back to Expense Tracker.'}
      </p>
      <form className="mt-6 space-y-4" onSubmit={submit}>
        {isRegistering && (
          <label className="block text-sm font-medium">
            Display name
            <input
              required
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              className="mt-1 block w-full rounded border border-slate-300 px-3 py-2 font-normal"
            />
          </label>
        )}
        <label className="block text-sm font-medium">
          Email
          <input
            required
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 px-3 py-2 font-normal"
          />
        </label>
        <label className="block text-sm font-medium">
          Password
          <input
            required
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 px-3 py-2 font-normal"
          />
        </label>
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded bg-slate-900 px-4 py-2 font-medium text-white hover:bg-slate-700 disabled:opacity-50"
        >
          {isSubmitting ? 'Please wait…' : isRegistering ? 'Create account' : 'Sign in'}
        </button>
      </form>
      <button
        type="button"
        className="mt-4 text-sm text-slate-600 underline"
        onClick={() => {
          setIsRegistering(!isRegistering);
          setError('');
        }}
      >
        {isRegistering ? 'Already have an account? Sign in' : 'Need an account? Register'}
      </button>
    </div>
  );
}
