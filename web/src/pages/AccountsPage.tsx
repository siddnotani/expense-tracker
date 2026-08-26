import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import ErrorBanner from '../components/ErrorBanner';
import {
  createAccount,
  listAccounts,
  queryKeys,
  updateAccount,
  type AccountType,
} from '../lib/api';
import { formatMinor, parseMajorToMinor } from '../lib/money';

export default function AccountsPage() {
  const queryClient = useQueryClient();
  const [includeArchived, setIncludeArchived] = useState(false);
  const [name, setName] = useState('');
  const [type, setType] = useState<AccountType>('CHECKING');
  const [currency, setCurrency] = useState('USD');
  const [openingBalance, setOpeningBalance] = useState('');
  const [formError, setFormError] = useState('');
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [rename, setRename] = useState('');

  const accounts = useQuery({
    queryKey: queryKeys.accounts(includeArchived),
    queryFn: () => listAccounts(includeArchived),
  });
  const invalidateAccounts = () => {
    void queryClient.invalidateQueries({ queryKey: ['accounts'] });
    void queryClient.invalidateQueries({ queryKey: ['transactionSummary'] });
  };
  const create = useMutation({
    mutationFn: createAccount,
    onSuccess: () => {
      setName('');
      setOpeningBalance('');
      setFormError('');
      invalidateAccounts();
    },
  });
  const update = useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: { name?: string; archived?: boolean } }) =>
      updateAccount(id, patch),
    onSuccess: () => {
      setRenamingId(null);
      invalidateAccounts();
    },
  });

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      create.mutate({
        name,
        type,
        currency: currency.toUpperCase(),
        openingBalanceMinor: openingBalance ? parseMajorToMinor(openingBalance) : 0,
      });
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Enter a valid opening balance.');
    }
  }

  function submitRename(event: FormEvent<HTMLFormElement>, id: string) {
    event.preventDefault();
    if (rename.trim()) update.mutate({ id, patch: { name: rename.trim() } });
  }

  return (
    <section className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold">Accounts</h2>
        <p className="mt-1 text-slate-600">Manage your accounts and balances.</p>
      </div>
      <ErrorBanner error={accounts.error ?? create.error ?? update.error} />
      {formError && <ErrorBanner error={new Error(formError)} />}

      <form className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm" onSubmit={submit}>
        <h3 className="text-lg font-semibold">Add account</h3>
        <div className="mt-4 grid gap-4 md:grid-cols-4">
          <label className="text-sm">
            Name
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              required
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </label>
          <label className="text-sm">
            Type
            <select
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              value={type}
              onChange={(event) => setType(event.target.value as AccountType)}
            >
              <option value="CHECKING">Checking</option>
              <option value="SAVINGS">Savings</option>
              <option value="CREDIT_CARD">Credit card</option>
              <option value="CASH">Cash</option>
            </select>
          </label>
          <label className="text-sm">
            Currency
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2 uppercase"
              maxLength={3}
              required
              value={currency}
              onChange={(event) => setCurrency(event.target.value)}
            />
          </label>
          <label className="text-sm">
            Opening balance
            <input
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              inputMode="decimal"
              placeholder="0.00"
              value={openingBalance}
              onChange={(event) => setOpeningBalance(event.target.value)}
            />
          </label>
        </div>
        <button
          className="mt-4 rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
          disabled={create.isPending}
          type="submit"
        >
          {create.isPending ? 'Adding…' : 'Add account'}
        </button>
      </form>

      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h3 className="text-lg font-semibold">Your accounts</h3>
          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              checked={includeArchived}
              onChange={(event) => setIncludeArchived(event.target.checked)}
            />
            Include archived
          </label>
        </div>
        {accounts.isLoading && <p className="mt-6 text-slate-600">Loading accounts…</p>}
        {!accounts.isLoading && accounts.data?.length === 0 && (
          <p className="mt-6 text-slate-600">No accounts yet. Add one above to get started.</p>
        )}
        <div className="mt-4 divide-y divide-slate-200">
          {accounts.data?.map((account) => (
            <div
              className="flex flex-wrap items-center justify-between gap-4 py-4"
              key={account.id}
            >
              <div className="min-w-0">
                {renamingId === account.id ? (
                  <form
                    className="flex gap-2"
                    onSubmit={(event) => submitRename(event, account.id)}
                  >
                    <input
                      className="rounded border border-slate-300 px-2 py-1"
                      value={rename}
                      onChange={(event) => setRename(event.target.value)}
                      autoFocus
                    />
                    <button className="text-sm font-medium text-slate-900" type="submit">
                      Save
                    </button>
                  </form>
                ) : (
                  <p className="font-medium">{account.name}</p>
                )}
                <p className="text-sm text-slate-600">
                  {account.type.replace('_', ' ')} · {account.currency}
                  {account.archived ? ' · Archived' : ''}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <p className="font-medium">{formatMinor(account.balanceMinor, account.currency)}</p>
                <button
                  className="text-sm text-slate-600 underline"
                  onClick={() => {
                    setRenamingId(account.id);
                    setRename(account.name);
                  }}
                  type="button"
                >
                  Rename
                </button>
                <button
                  className="text-sm text-slate-600 underline"
                  onClick={() =>
                    update.mutate({ id: account.id, patch: { archived: !account.archived } })
                  }
                  type="button"
                >
                  {account.archived ? 'Unarchive' : 'Archive'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
