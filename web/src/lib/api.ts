export type Category = {
  id: string;
  name: string;
  color: string | null;
  archived: boolean;
};

export type Budget = {
  id: string;
  categoryId: string;
  categoryName: string;
  amountMinor: number;
};

export type ProgressCategory = {
  categoryId: string | null;
  categoryName: string;
  budgetedMinor: number;
  spentMinor: number;
  remainingMinor: number;
  overBudget: boolean;
};

export type BudgetProgress = {
  period: string;
  totalBudgetedMinor: number;
  totalSpentMinor: number;
  remainingMinor: number;
  categories: ProgressCategory[];
};

export type Dashboard = {
  totalSpentMinor: number;
  totalIncomeMinor: number;
  netMinor: number;
  topCategories: { categoryId: string | null; categoryName: string; spentMinor: number }[];
  budgetProgress: BudgetProgress;
};

function xsrfToken(): string | null {
  const cookie = document.cookie.split('; ').find((value) => value.startsWith('XSRF-TOKEN='));
  return cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : null;
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const token = xsrfToken();
  if (token) {
    headers.set('X-XSRF-TOKEN', token);
  }
  const response = await fetch(path, { ...options, credentials: 'include', headers });
  if (!response.ok) {
    const detail = await response.text();
    throw new Error(detail || `Request failed with status ${response.status}`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function currentMonth(): string {
  const date = new Date();
  return formatMonth(date);
}

export function formatMonth(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

export function formatMinorUnits(minor: number): string {
  const sign = minor < 0 ? '-' : '';
  const absolute = Math.abs(Math.trunc(minor));
  const units = Math.floor(absolute / 100);
  const cents = String(absolute % 100).padStart(2, '0');
  return `${sign}${units}.${cents}`;
}

export function parseMajorUnits(value: string): number {
  const normalized = value.trim();
  if (!/^-?(?:\d+|\d+\.\d{1,2})$/.test(normalized)) {
    throw new Error('Enter an amount with up to two decimal places');
  }
  const negative = normalized.startsWith('-');
  const unsigned = negative ? normalized.slice(1) : normalized;
  const [units, cents = ''] = unsigned.split('.');
  const minor = BigInt(units) * 100n + BigInt(cents.padEnd(2, '0'));
  const result = negative ? -minor : minor;
  const number = Number(result);
  if (!Number.isSafeInteger(number)) {
    throw new Error('Amount is too large');
  }
  return number;
}

export type CreateCategoryRequest = { name: string; color?: string };
export type UpdateCategoryRequest = { name?: string; color?: string; archived?: boolean };
export type UpsertBudgetRequest = {
  categoryId: string;
  period: string;
  amountMinor: number;
};
