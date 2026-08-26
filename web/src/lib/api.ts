export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

function csrfToken(): string | undefined {
  const cookie = document.cookie.split('; ').find((entry) => entry.startsWith('XSRF-TOKEN='));
  return cookie ? decodeURIComponent(cookie.substring('XSRF-TOKEN='.length)) : undefined;
}

export async function api<T = unknown>(
  path: string,
  options: { method?: string; body?: unknown } = {},
): Promise<T | null> {
  const method = options.method ?? 'GET';
  const headers = new Headers();
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  if (method !== 'GET') {
    const token = csrfToken();
    if (token) {
      headers.set('X-XSRF-TOKEN', token);
    }
  }

  const response = await fetch(path, {
    method,
    credentials: 'include',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  if (response.status === 204) {
    return null;
  }
  if (!response.ok) {
    let message = response.statusText || 'Request failed';
    try {
      const problem = (await response.json()) as { detail?: string; title?: string };
      message = problem.detail ?? problem.title ?? message;
    } catch {
      // Keep the status text when the response is not JSON.
    }
    throw new ApiError(message, response.status);
  }
  return (await response.json()) as T;
}

export type AccountType = 'CHECKING' | 'SAVINGS' | 'CREDIT_CARD' | 'CASH';
export type Direction = 'EXPENSE' | 'INCOME';

export type Account = {
  id: string;
  name: string;
  type: AccountType;
  currency: string;
  balanceMinor: number;
  archived: boolean;
  createdAt: string;
};

export type AccountInput = {
  name: string;
  type: AccountType;
  currency: string;
  openingBalanceMinor: number;
};

export type AccountPatch = {
  name?: string;
  archived?: boolean;
};

export type Transaction = {
  id: string;
  accountId: string;
  categoryId: string | null;
  direction: Direction;
  amountMinor: number;
  currency: string;
  occurredOn: string;
  description: string;
  createdAt: string;
};

export type TransactionInput = {
  accountId: string;
  categoryId?: string;
  direction: Direction;
  amountMinor: number;
  currency: string;
  occurredOn: string;
  description: string;
};

export type TransactionFilters = {
  from: string;
  to: string;
  accountId: string;
  categoryId: string;
  direction: string;
  page: number;
  size: number;
};

export type TransactionPage = {
  items: Transaction[];
  total: number;
  page: number;
  size: number;
};

export type TransactionSummary = {
  totalExpenseMinor: number;
  totalIncomeMinor: number;
  netMinor: number;
  byCategory: { categoryId: string | null; totalMinor: number }[];
};

export const queryKeys = {
  accounts: (includeArchived: boolean) => ['accounts', { includeArchived }] as const,
  transactions: (filters: TransactionFilters) => ['transactions', filters] as const,
  transactionSummary: (range: { from: string; to: string }) =>
    ['transactionSummary', range] as const,
};

export function listAccounts(includeArchived: boolean): Promise<Account[]> {
  return api<Account[]>(`/api/accounts?includeArchived=${String(includeArchived)}`) as Promise<
    Account[]
  >;
}

export function createAccount(input: AccountInput): Promise<Account> {
  return api<Account>('/api/accounts', {
    method: 'POST',
    body: input,
  }) as Promise<Account>;
}

export function updateAccount(id: string, patch: AccountPatch): Promise<Account> {
  return api<Account>(`/api/accounts/${id}`, {
    method: 'PATCH',
    body: patch,
  }) as Promise<Account>;
}

export function listTransactions(filters: TransactionFilters): Promise<TransactionPage> {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '') {
      params.set(key, String(value));
    }
  });
  return api<TransactionPage>(`/api/transactions?${params.toString()}`) as Promise<TransactionPage>;
}

export function createTransaction(input: TransactionInput): Promise<Transaction> {
  return api<Transaction>('/api/transactions', {
    method: 'POST',
    body: input,
  }) as Promise<Transaction>;
}

export function updateTransaction(id: string, input: TransactionInput): Promise<Transaction> {
  return api<Transaction>(`/api/transactions/${id}`, {
    method: 'PUT',
    body: input,
  }) as Promise<Transaction>;
}

export function deleteTransaction(id: string): Promise<void> {
  return api<void>(`/api/transactions/${id}`, { method: 'DELETE' }).then(() => undefined);
}

export function getTransactionSummary(range: {
  from: string;
  to: string;
}): Promise<TransactionSummary> {
  const params = new URLSearchParams();
  if (range.from) params.set('from', range.from);
  if (range.to) params.set('to', range.to);
  return api<TransactionSummary>(
    `/api/transactions/summary?${params.toString()}`,
  ) as Promise<TransactionSummary>;
}
