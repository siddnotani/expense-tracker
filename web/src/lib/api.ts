import { request } from './apiClient';

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
  return request<Account[]>(`/api/accounts?includeArchived=${String(includeArchived)}`);
}

export function createAccount(input: AccountInput): Promise<Account> {
  return request<Account>('/api/accounts', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function updateAccount(id: string, patch: AccountPatch): Promise<Account> {
  return request<Account>(`/api/accounts/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(patch),
  });
}

export function listTransactions(filters: TransactionFilters): Promise<TransactionPage> {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '') {
      params.set(key, String(value));
    }
  });
  return request<TransactionPage>(`/api/transactions?${params.toString()}`);
}

export function createTransaction(input: TransactionInput): Promise<Transaction> {
  return request<Transaction>('/api/transactions', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function updateTransaction(id: string, input: TransactionInput): Promise<Transaction> {
  return request<Transaction>(`/api/transactions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function deleteTransaction(id: string): Promise<void> {
  return request<void>(`/api/transactions/${id}`, { method: 'DELETE' });
}

export function getTransactionSummary(range: {
  from: string;
  to: string;
}): Promise<TransactionSummary> {
  const params = new URLSearchParams();
  if (range.from) params.set('from', range.from);
  if (range.to) params.set('to', range.to);
  return request<TransactionSummary>(`/api/transactions/summary?${params.toString()}`);
}
