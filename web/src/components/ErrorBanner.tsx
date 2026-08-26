import { ApiError } from '../lib/api';

type ErrorBannerProps = {
  error: unknown;
};

export default function ErrorBanner({ error }: ErrorBannerProps) {
  if (!error) return null;
  const detail =
    error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
  return (
    <div
      className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800"
      role="alert"
    >
      {detail}
    </div>
  );
}
