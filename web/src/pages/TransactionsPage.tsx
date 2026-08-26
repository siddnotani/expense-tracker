import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState, type FormEvent } from 'react';
import ErrorBanner from '../components/ErrorBanner';
import {
  createTransaction,
  deleteTransaction,
  listAccounts,
  listTransactions,
  queryKeys,
  getTransactionSummary,
  updateTransaction,
  type Direction,
  type Transaction,
  type TransactionFilters,
} from '../lib/api';
import { formatMinor, minorToMajorInput, parseMajorToMinor, signedMinor } from '../lib/money';

const initialFilters: TransactionFilters = {
  from: '',
  to: '',
  accountId: '',
  categoryId: '',
  direction: '',
  page: 0,
  size: 50,
};

const today = () => new Date().toISOString().slice(0, 10);

type FormState = {
  accountId: string;
  categoryId: string;
  direction: Direction;
  amount: string;
  currency: string;
  occurredOn: string;
  description: string;
};

const emptyForm = (): FormState => ({
  accountId: '',
  categoryId: '',
  direction: 'EXPENSE',
  amount: '',
  currency: 'USD',
  occurredOn: today(),
  description: '',
});

export default function TransactionsPage() {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState(initialFilters);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formError, setFormError] = useState('');
  const accounts = useQuery({
    queryKey: queryKeys.accounts(false),
    queryFn: () => listAccounts(false),
  });
  const transactions = useQuery({
    queryKey: queryKeys.transactions(filters),
    queryFn: () => listTransactions(filters),
  });
  const summaryRange = { from: filters.from, to: filters.to };
  const summary = useQuery({
    queryKey: queryKeys.transactionSummary(summaryRange),
    queryFn: () => getTransactionSummary(summaryRange),
  });
  const accountNames = useMemo(
    () => new Map(accounts.data?.map((account) => [account.id, account.name])),
    [accounts.data],
  );
  const invalidateAll = () => {
    void queryClient.invalidateQueries({ queryKey: ['transactions'] });
    void queryClient.invalidateQueries({ queryKey: ['accounts'] });
    void queryClient.invalidateQueries({ queryKey: ['transactionSummary'] });
  };
  const save = useMutation({
    mutationFn: (input: { id?: string; transaction: ReturnType<typeof toInput> }) =>
      input.id
        ? updateTransaction(input.id, input.transaction)
        : createTransaction(input.transaction),
    onSuccess: () => {
      setForm(emptyForm());
      setEditingId(null);
      setFormError('');
      invalidateAll();
    },
  });
  const remove = useMutation({
    mutationFn: deleteTransaction,
    onSuccess: invalidateAll,
  });

  function toInput(): {
    accountId: string;
    categoryId?: string;
    direction: Direction;
    amountMinor: number;
    currency: string;
    occurredOn: string;
    description: string;
  } {
    return {
      accountId: form.accountId,
      categoryId: form.categoryId || undefined,
      direction: form.direction,
      amountMinor: parseMajorToMinor(form.amount),
      currency: form.currency.toUpperCase(),
      occurredOn: form.occurredOn,
      description: form.description,
    };
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      save.mutate({ id: editingId ?? undefined, transaction: toInput() });
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Enter a valid amount.');
    }
  }

  function startEdit(transaction: Transaction) {
    setEditingId(transaction.id);
    setForm({
      accountId: transaction.accountId,
      categoryId: transaction.categoryId ?? '',
      direction: transaction.direction,
      amount: minorToMajorInput(transaction.amountMinor),
      currency: transaction.currency,
      occurredOn: transaction.occurredOn,
      description: transaction.description,
    });
    setFormError('');
  }

  function updateFilter<K extends keyof TransactionFilters>(key: K, value: TransactionFilters[K]) {
    setFilters((current) => ({
      ...current,
      [key]: value,
      page: key === 'page' ? (value as number) : 0,
    }));
  }

  return (
    <section className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold">Transactions</h2>
        <p className="mt-1 text-slate-600">Track income and expenses in one place.</p>
      </div>
      <ErrorBanner
        error={transactions.error ?? summary.error ?? accounts.error ?? save.error ?? remove.error}
      />
      {formError && <ErrorBanner error={new Error(formError)} />}

      <form className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm" onSubmit={submit}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h3 className="text-lg font-semibold">
            {editingId ? 'Edit transaction' : 'Add transaction'}
          </h3>
          {editingId && (
            <button
              className="text-sm text-slate-600 underline"
              onClick={() => {
                setEditingId(null);
                setForm(emptyForm());
              }}
              type="button"
            >
              Cancel edit
            </button>
          )}
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-4">
          <label className="text-sm">
            Account
            <select
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              required
              value={form.accountId}
              onChange={(event) => setForm({ ...form, accountId: event.target.value })}
            >
              <option value="">Choose an account</option>
              {accounts.data?.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm">
            Direction
            <select
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              value={form.direction}
              onChange={(event) => setForm({ ...form, direction: event.target.value as Direction })}
            >
              <option value="EXPENSE">Expense</option>
              <option value="INCOME">Income</option>
            </select>
          </label>
          <label className="text-sm">
            Amount
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              inputMode="decimal"
              placeholder="0.00"
              required
              value={form.amount}
              onChange={(event) => setForm({ ...form, amount: event.target.value })}
            />
          </label>
          <label className="text-sm">
            Currency
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2 uppercase"
              maxLength={3}
              required
              value={form.currency}
              onChange={(event) => setForm({ ...form, currency: event.target.value })}
            />
          </label>
          <label className="text-sm">
            Date
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              required
              type="date"
              value={form.occurredOn}
              onChange={(event) => setForm({ ...form, occurredOn: event.target.value })}
            />
          </label>
          <label className="text-sm">
            Category ID
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              value={form.categoryId}
              onChange={(event) => setForm({ ...form, categoryId: event.target.value })}
            />
          </label>
          <label className="text-sm md:col-span-2">
            Description
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              required
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
            />
          </label>
        </div>
        <button
          className="mt-4 rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
          disabled={save.isPending}
          type="submit"
        >
          {save.isPending ? 'Saving…' : editingId ? 'Save changes' : 'Add transaction'}
        </button>
      </form>

      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="text-lg font-semibold">Filters</h3>
        <div className="mt-4 grid gap-4 md:grid-cols-6">
          <label className="text-sm">
            From
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              type="date"
              value={filters.from}
              onChange={(event) => updateFilter('from', event.target.value)}
            />
          </label>
          <label className="text-sm">
            To
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              type="date"
              value={filters.to}
              onChange={(event) => updateFilter('to', event.target.value)}
            />
          </label>
          <label className="text-sm">
            Account
            <select
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              value={filters.accountId}
              onChange={(event) => updateFilter('accountId', event.target.value)}
            >
              <option value="">All accounts</option>
              {accounts.data?.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm">
            Direction
            <select
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              value={filters.direction}
              onChange={(event) => updateFilter('direction', event.target.value)}
            >
              <option value="">All directions</option>
              <option value="EXPENSE">Expenses</option>
              <option value="INCOME">Income</option>
            </select>
          </label>
          <label className="text-sm md:col-span-2">
            Category ID
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              value={filters.categoryId}
              onChange={(event) => updateFilter('categoryId', event.target.value)}
            />
          </label>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        {[
          ['Expenses', summary.data?.totalExpenseMinor ?? 0],
          ['Income', summary.data?.totalIncomeMinor ?? 0],
          ['Net', summary.data?.netMinor ?? 0],
        ].map(([label, value]) => (
          <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm" key={label}>
            <p className="text-sm text-slate-600">{label}</p>
            <p className="mt-2 text-xl font-semibold">{formatMinor(Number(value), 'USD')}</p>
          </div>
        ))}
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white shadow-sm">
        {transactions.isLoading ? (
          <p className="p-6 text-slate-600">Loading transactions…</p>
        ) : transactions.data?.items.length === 0 ? (
          <p className="p-6 text-slate-600">No transactions match these filters.</p>
        ) : (
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-slate-600">
              <tr>
                <th className="px-4 py-3 font-medium">Date</th>
                <th className="px-4 py-3 font-medium">Description</th>
                <th className="px-4 py-3 font-medium">Account</th>
                <th className="px-4 py-3 font-medium">Category ID</th>
                <th className="px-4 py-3 text-right font-medium">Amount</th>
                <th className="px-4 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {transactions.data?.items.map((transaction) => (
                <tr key={transaction.id}>
                  <td className="whitespace-nowrap px-4 py-3">{transaction.occurredOn}</td>
                  <td className="px-4 py-3">{transaction.description}</td>
                  <td className="px-4 py-3">
                    {accountNames.get(transaction.accountId) ?? 'Unknown'}
                  </td>
                  <td className="px-4 py-3 text-slate-600">{transaction.categoryId ?? '—'}</td>
                  <td
                    className={`whitespace-nowrap px-4 py-3 text-right ${
                      transaction.direction === 'EXPENSE' ? 'text-slate-900' : 'text-emerald-700'
                    }`}
                  >
                    {formatMinor(
                      signedMinor(transaction.direction, transaction.amountMinor),
                      transaction.currency,
                    )}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <button
                      className="mr-3 text-slate-600 underline"
                      onClick={() => startEdit(transaction)}
                      type="button"
                    >
                      Edit
                    </button>
                    <button
                      className="text-slate-600 underline"
                      onClick={() => {
                        if (window.confirm('Delete this transaction?'))
                          remove.mutate(transaction.id);
                      }}
                      type="button"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
          <span>{transactions.data?.total ?? 0} total transactions</span>
          <div className="flex items-center gap-3">
            <label>
              Page size
              <select
                className="ml-2 rounded border border-slate-300 px-2 py-1"
                value={filters.size}
                onChange={(event) => updateFilter('size', Number(event.target.value))}
              >
                <option value={25}>25</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
              </select>
            </label>
            <button
              className="rounded border border-slate-300 px-3 py-1 disabled:opacity-40"
              disabled={filters.page === 0}
              onClick={() => updateFilter('page', filters.page - 1)}
              type="button"
            >
              Previous
            </button>
            <span>Page {filters.page + 1}</span>
            <button
              className="rounded border border-slate-300 px-3 py-1 disabled:opacity-40"
              disabled={
                !transactions.data || (filters.page + 1) * filters.size >= transactions.data.total
              }
              onClick={() => updateFilter('page', filters.page + 1)}
              type="button"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
