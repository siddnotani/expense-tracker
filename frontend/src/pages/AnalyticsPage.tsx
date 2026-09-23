import { useCallback, useEffect, useState } from 'react'
import { getAnalyticsSummary } from '../api/analytics'
import type { AnalyticsSummaryResponse } from '../types/analytics'
import { StatCard } from '../components/StatCard'
import { CategoryPieChart } from '../components/CategoryPieChart'
import { MonthlyBarChart } from '../components/MonthlyBarChart'
import { formatCurrency } from '../utils/format'

interface Query {
  from: string
  to: string
  reloadKey: number
}

type Result =
  | { status: 'error'; message: string }
  | { status: 'success'; data: AnalyticsSummaryResponse }

type LoadState = { status: 'loading' } | Result

export function AnalyticsPage() {
  const [query, setQuery] = useState<Query>({ from: '', to: '', reloadKey: 0 })
  const [resolved, setResolved] = useState<{ query: Query; result: Result } | null>(null)
  const { from, to } = query

  const setFrom = (from: string) => setQuery((q) => ({ ...q, from }))
  const setTo = (to: string) => setQuery((q) => ({ ...q, to }))
  const retry = useCallback(
    () => setQuery((q) => ({ ...q, reloadKey: q.reloadKey + 1 })),
    [],
  )

  useEffect(() => {
    let cancelled = false
    getAnalyticsSummary({ from: query.from || undefined, to: query.to || undefined })
      .then((data) => {
        if (!cancelled) setResolved({ query, result: { status: 'success', data } })
      })
      .catch((err: unknown) => {
        if (cancelled) return
        const message = err instanceof Error ? err.message : 'Unknown error'
        setResolved({ query, result: { status: 'error', message } })
      })
    return () => {
      cancelled = true
    }
  }, [query])

  const state: LoadState =
    resolved && resolved.query === query ? resolved.result : { status: 'loading' }

  return (
    <section className="page">
      <div className="page-header">
        <h1>Analytics</h1>
        <form className="filters" onSubmit={(e) => e.preventDefault()}>
          <label>
            From
            <input
              type="date"
              value={from}
              max={to || undefined}
              onChange={(e) => setFrom(e.target.value)}
            />
          </label>
          <label>
            To
            <input
              type="date"
              value={to}
              min={from || undefined}
              onChange={(e) => setTo(e.target.value)}
            />
          </label>
          {(from || to) && (
            <button
              type="button"
              className="button button--ghost"
              onClick={() => setQuery((q) => ({ ...q, from: '', to: '' }))}
            >
              Clear
            </button>
          )}
        </form>
      </div>

      {state.status === 'loading' && <LoadingSkeleton />}

      {state.status === 'error' && (
        <div className="state state--error" role="alert">
          <p>Could not load analytics: {state.message}</p>
          <button type="button" className="button" onClick={retry}>
            Retry
          </button>
        </div>
      )}

      {state.status === 'success' && state.data.stats.count === 0 && (
        <div className="state state--empty">
          <p>No expenses found{from || to ? ' for the selected period' : ''}.</p>
        </div>
      )}

      {state.status === 'success' && state.data.stats.count > 0 && (
        <Summary data={state.data} />
      )}
    </section>
  )
}

function Summary({ data }: { data: AnalyticsSummaryResponse }) {
  return (
    <>
      <div className="stats-grid">
        <StatCard label="Total spend" value={formatCurrency(data.totalSpend)} />
        <StatCard label="Expenses" value={String(data.stats.count)} />
        <StatCard label="Average" value={formatCurrency(data.stats.average)} />
        <StatCard label="Min" value={formatCurrency(data.stats.min)} />
        <StatCard label="Max" value={formatCurrency(data.stats.max)} />
      </div>
      <div className="charts-grid">
        <div className="card">
          <h2>Spend by category</h2>
          <CategoryPieChart data={data.byCategory} />
        </div>
        <div className="card">
          <h2>Spend by month</h2>
          <MonthlyBarChart data={data.byMonth} />
        </div>
      </div>
    </>
  )
}

function LoadingSkeleton() {
  return (
    <div className="state state--loading" role="status" aria-live="polite" aria-label="Loading">
      <div className="stats-grid">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="stat-card skeleton" />
        ))}
      </div>
      <div className="charts-grid">
        <div className="card skeleton skeleton--chart" />
        <div className="card skeleton skeleton--chart" />
      </div>
    </div>
  )
}
