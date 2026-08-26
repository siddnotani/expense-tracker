const DEV_USER_ID = '00000000-0000-0000-0000-000000000001';

export class ApiError extends Error {
  status: number;
  detail: string;

  constructor(status: number, detail: string) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.detail = detail;
  }
}

function cookieValue(name: string): string | undefined {
  const cookie = document.cookie.split('; ').find((entry) => entry.startsWith(`${name}=`));
  return cookie ? decodeURIComponent(cookie.slice(name.length + 1)) : undefined;
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  // TEMPORARY-SEAM: forward the dev identity until the sibling auth PR lands.
  headers.set('X-User-Id', DEV_USER_ID);
  const xsrfToken = cookieValue('XSRF-TOKEN');
  if (xsrfToken) {
    headers.set('X-XSRF-TOKEN', xsrfToken);
  }

  const response = await fetch(path, {
    ...options,
    credentials: 'include',
    headers,
  });
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as {
      detail?: string;
      title?: string;
    };
    throw new ApiError(
      response.status,
      problem.detail ?? problem.title ?? `Request failed with status ${response.status}`,
    );
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
