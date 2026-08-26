import { useQuery } from '@tanstack/react-query';
import { apiFetch, currentMonth, Dashboard as DashboardData, formatMinorUnits } from '../lib/api';

export default function DashboardPage() {
  const period = currentMonth();
  const dashboard = useQuery({
    queryKey: ['dashboard', period],
    queryFn: () => apiFetch<DashboardData>(`/api/dashboard?period=${period}`),
  });

  if (dashboard.isLoading) {
    return <p className="text-sm text-slate-500">Loading your dashboard…</p>;
  }
  if (dashboard.isError || !dashboard.data) {
    return (
      <p className="text-sm text-red-700">We couldn't load your dashboard. Please try again.</p>
    );
  }
  const data = dashboard.data;
  const brandNew =
    data.totalSpentMinor === 0 &&
    data.totalIncomeMinor === 0 &&
    data.budgetProgress.categories.length === 0;
  const maxSpend = Math.max(...data.topCategories.map((category) => category.spentMinor), 1);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold">Dashboard</h2>
        <p className="mt-1 text-sm text-slate-600">Your expense overview for {period}.</p>
      </div>
      {brandNew ? (
        <section className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center shadow-sm">
          <h3 className="font-medium">Welcome to your expense tracker</h3>
          <p className="mx-auto mt-2 max-w-lg text-sm text-slate-600">
            Start by creating an account, adding transactions, and setting budgets so your dashboard
            can show your progress.
          </p>
        </section>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            <Metric label="Spent" value={formatMinorUnits(data.totalSpentMinor)} />
            <Metric label="Income" value={formatMinorUnits(data.totalIncomeMinor)} />
            <Metric label="Net" value={formatMinorUnits(data.netMinor)} />
          </div>
          <div className="grid gap-6 lg:grid-cols-2">
            <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
              <h3 className="font-medium">Top categories</h3>
              <div className="mt-4 space-y-4">
                {data.topCategories.length === 0 ? (
                  <p className="text-sm text-slate-500">No spending this month.</p>
                ) : (
                  data.topCategories.map((category) => (
                    <div key={`${category.categoryId ?? 'null'}-${category.categoryName}`}>
                      <div className="mb-1 flex justify-between text-sm">
                        <span>{category.categoryName}</span>
                        <span className="text-slate-600">
                          {formatMinorUnits(category.spentMinor)}
                        </span>
                      </div>
                      <div className="h-2 rounded-full bg-slate-100">
                        <div
                          className="h-2 rounded-full bg-slate-700"
                          style={{ width: `${(category.spentMinor / maxSpend) * 100}%` }}
                        />
                      </div>
                    </div>
                  ))
                )}
              </div>
            </section>
            <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
              <h3 className="font-medium">Budget progress</h3>
              <div className="mt-4 space-y-4">
                {data.budgetProgress.categories.length === 0 ? (
                  <p className="text-sm text-slate-500">No budgets or spending categories yet.</p>
                ) : (
                  data.budgetProgress.categories.map((category) => (
                    <div key={`${category.categoryId ?? 'null'}-${category.categoryName}`}>
                      <div className="mb-1 flex justify-between text-sm">
                        <span>{category.categoryName}</span>
                        <span
                          className={
                            category.overBudget ? 'font-semibold text-red-700' : 'text-slate-600'
                          }
                        >
                          {category.overBudget
                            ? 'Over budget'
                            : `${formatMinorUnits(category.remainingMinor)} left`}
                        </span>
                      </div>
                      <div className="h-2 rounded-full bg-slate-100">
                        <div
                          className={`h-2 rounded-full ${category.overBudget ? 'bg-red-600' : 'bg-slate-700'}`}
                          style={{
                            width: `${category.budgetedMinor > 0 ? Math.min((category.spentMinor / category.budgetedMinor) * 100, 100) : 100}%`,
                          }}
                        />
                      </div>
                    </div>
                  ))
                )}
              </div>
            </section>
          </div>
        </>
      )}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-semibold">{value}</p>
    </section>
  );
}
