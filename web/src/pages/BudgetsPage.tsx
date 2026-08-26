import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import {
  Budget,
  Category,
  currentMonth,
  formatMonth,
  createCategory as createCategoryRequest,
  createDefaultCategories,
  copyBudgets,
  getBudgetProgress,
  listBudgets,
  listCategories,
  updateCategory as updateCategoryRequest,
  upsertBudget,
} from '../lib/api';
import { formatMinor, minorToMajorInput, parseMajorToMinor } from '../lib/money';

export default function BudgetsPage() {
  const queryClient = useQueryClient();
  const [period, setPeriod] = useState(currentMonth);
  const [newName, setNewName] = useState('');
  const [newColor, setNewColor] = useState('#64748b');
  const categories = useQuery({
    queryKey: ['categories'],
    queryFn: () => listCategories(),
  });
  const budgets = useQuery({
    queryKey: ['budgets', period],
    queryFn: () => listBudgets(period),
  });
  const progress = useQuery({
    queryKey: ['budget-progress', period],
    queryFn: () => getBudgetProgress(period),
  });

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: ['categories'] });
    void queryClient.invalidateQueries({ queryKey: ['budgets'] });
    void queryClient.invalidateQueries({ queryKey: ['budget-progress'] });
    void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
  };
  const createCategory = useMutation({
    mutationFn: () => createCategoryRequest({ name: newName, color: newColor }),
    onSuccess: () => {
      setNewName('');
      refresh();
    },
  });
  const defaults = useMutation({
    mutationFn: createDefaultCategories,
    onSuccess: refresh,
  });
  const updateCategory = useMutation({
    mutationFn: ({
      id,
      body,
    }: {
      id: string;
      body: { name?: string; color?: string; archived?: boolean };
    }) => updateCategoryRequest(id, body),
    onSuccess: refresh,
  });
  const updateBudget = useMutation({
    mutationFn: (body: { categoryId: string; amountMinor: number }) =>
      upsertBudget({ ...body, period }),
    onSuccess: refresh,
  });
  const copyLastMonth = useMutation({
    mutationFn: () => {
      const date = new Date(`${period}-01T00:00:00`);
      date.setMonth(date.getMonth() - 1);
      return copyBudgets({ fromPeriod: formatMonth(date), toPeriod: period });
    },
    onSuccess: refresh,
  });

  const budgetByCategory = new Map(
    (budgets.data ?? []).map((budget) => [budget.categoryId, budget]),
  );
  const spentByCategory = new Map(
    (progress.data?.categories ?? []).map((category) => [category.categoryId, category.spentMinor]),
  );
  const totalBudgeted = (budgets.data ?? []).reduce((sum, budget) => sum + budget.amountMinor, 0);
  const totalSpent = (progress.data?.categories ?? []).reduce(
    (sum, category) => sum + category.spentMinor,
    0,
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 className="text-2xl font-semibold">Budgets</h2>
          <p className="mt-1 text-sm text-slate-600">Plan monthly spending by category.</p>
        </div>
        <div className="flex items-end gap-3">
          <label className="text-sm text-slate-600">
            Month
            <input
              type="month"
              value={period}
              onChange={(event) => setPeriod(event.target.value)}
              className="mt-1 block rounded border border-slate-300 bg-white px-3 py-2 text-slate-900"
            />
          </label>
          <button
            type="button"
            onClick={() => copyLastMonth.mutate()}
            className="rounded bg-slate-800 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700"
          >
            Copy last month
          </button>
        </div>
      </div>

      <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="font-medium">Monthly plan</h3>
        {categories.isLoading || budgets.isLoading ? (
          <p className="mt-4 text-sm text-slate-500">Loading your categories…</p>
        ) : categories.data?.length === 0 ? (
          <div className="mt-4 rounded border border-dashed border-slate-300 p-5 text-sm text-slate-600">
            <p>You have no categories yet.</p>
            <button
              type="button"
              onClick={() => defaults.mutate()}
              className="mt-3 rounded bg-slate-800 px-3 py-2 font-medium text-white hover:bg-slate-700"
            >
              Create default categories
            </button>
          </div>
        ) : (
          <div className="mt-4 divide-y divide-slate-100">
            {(categories.data ?? []).map((category) => (
              <BudgetRow
                key={category.id}
                category={category}
                budget={budgetByCategory.get(category.id)}
                spent={spentByCategory.get(category.id) ?? 0}
                onBudgetChange={(amountMinor) =>
                  updateBudget.mutate({ categoryId: category.id, amountMinor })
                }
                onUpdate={(body) => updateCategory.mutate({ id: category.id, body })}
              />
            ))}
            <div className="flex justify-between pt-4 text-sm font-semibold">
              <span>Total</span>
              <span>
                {formatMinor(totalSpent, 'USD')} spent / {formatMinor(totalBudgeted, 'USD')}{' '}
                budgeted
              </span>
            </div>
          </div>
        )}
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="font-medium">Manage categories</h3>
        <form
          className="mt-4 flex flex-wrap items-end gap-3"
          onSubmit={(event) => {
            event.preventDefault();
            if (newName.trim()) createCategory.mutate();
          }}
        >
          <label className="text-sm text-slate-600">
            New category
            <input
              value={newName}
              onChange={(event) => setNewName(event.target.value)}
              className="mt-1 block rounded border border-slate-300 px-3 py-2"
              placeholder="Category name"
            />
          </label>
          <label className="text-sm text-slate-600">
            Color
            <input
              type="color"
              value={newColor}
              onChange={(event) => setNewColor(event.target.value)}
              className="mt-1 block h-10 w-14 rounded border border-slate-300 bg-white p-1"
            />
          </label>
          <button
            type="submit"
            className="rounded bg-slate-800 px-3 py-2 text-sm font-medium text-white"
          >
            Add category
          </button>
        </form>
      </section>
    </div>
  );
}

function BudgetRow({
  category,
  budget,
  spent,
  onBudgetChange,
  onUpdate,
}: {
  category: Category;
  budget?: Budget;
  spent: number;
  onBudgetChange: (amountMinor: number) => void;
  onUpdate: (body: { name?: string; color?: string; archived?: boolean }) => void;
}) {
  const [amount, setAmount] = useState(minorToMajorInput(budget?.amountMinor ?? 0));
  const [name, setName] = useState(category.name);
  const budgeted = budget?.amountMinor ?? 0;
  useEffect(() => {
    setAmount(minorToMajorInput(budgeted));
  }, [budgeted]);
  const ratio = budgeted > 0 ? Math.min((spent / budgeted) * 100, 100) : spent > 0 ? 100 : 0;
  const overBudget = spent > budgeted;
  return (
    <div className="space-y-3 py-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex min-w-48 items-center gap-2">
          <span
            className="h-3 w-3 rounded-full"
            style={{ backgroundColor: category.color ?? '#64748b' }}
          />
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            onBlur={() => name.trim() && name !== category.name && onUpdate({ name: name.trim() })}
            className="min-w-0 rounded border border-transparent px-1 py-1 font-medium hover:border-slate-300 focus:border-slate-400"
          />
          <input
            type="color"
            value={category.color ?? '#64748b'}
            onChange={(event) => onUpdate({ color: event.target.value })}
            className="h-7 w-8 rounded border border-slate-300 bg-white p-0.5"
            aria-label={`Color for ${category.name}`}
          />
          <button
            type="button"
            onClick={() => onUpdate({ archived: true })}
            className="text-xs text-slate-500 hover:text-slate-900"
          >
            Archive
          </button>
        </div>
        <label className="text-sm text-slate-600">
          Budget
          <input
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
            onBlur={() => {
              try {
                onBudgetChange(parseMajorToMinor(amount));
              } catch {
                setAmount(minorToMajorInput(budgeted));
              }
            }}
            className="ml-2 w-28 rounded border border-slate-300 px-2 py-1 text-right text-slate-900"
            inputMode="decimal"
            aria-label={`Budget for ${category.name}`}
          />
        </label>
      </div>
      <div>
        <div className="mb-1 flex justify-between text-xs text-slate-500">
          <span>{formatMinor(spent, 'USD')} spent</span>
          <span className={overBudget ? 'font-semibold text-red-700' : ''}>
            {overBudget
              ? 'Over budget'
              : `${formatMinor(Math.max(budgeted - spent, 0), 'USD')} remaining`}
          </span>
        </div>
        <div className="h-2 overflow-hidden rounded-full bg-slate-100">
          <div
            className={`h-full rounded-full ${overBudget ? 'bg-red-600' : 'bg-slate-700'}`}
            style={{ width: `${ratio}%` }}
          />
        </div>
      </div>
    </div>
  );
}
