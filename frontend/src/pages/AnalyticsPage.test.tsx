import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AnalyticsPage } from './AnalyticsPage'
import type { AnalyticsSummaryResponse } from '../types/analytics'

const summary: AnalyticsSummaryResponse = {
  totalSpend: 1234.56,
  stats: { count: 40, average: 30.86, min: 2.5, max: 420 },
  byCategory: [
    { category: 'FOOD', displayName: 'Food', total: 300, percentage: 24.3 },
    { category: 'HOUSING', displayName: 'Housing', total: 934.56, percentage: 75.7 },
  ],
  byMonth: [
    { month: '2026-04', total: 210 },
    { month: '2026-05', total: 1024.56 },
  ],
}

const emptySummary: AnalyticsSummaryResponse = {
  totalSpend: 0,
  stats: { count: 0, average: 0, min: 0, max: 0 },
  byCategory: [],
  byMonth: [],
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('AnalyticsPage', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('shows a loading state and then renders stats from the API', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(summary))
    render(<AnalyticsPage />)

    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument()

    expect(await screen.findByText('$1,234.56')).toBeInTheDocument()
    expect(screen.getByText('40')).toBeInTheDocument()
    expect(screen.getByText('$30.86')).toBeInTheDocument()
    expect(screen.getByText('$2.50')).toBeInTheDocument()
    expect(screen.getByText('$420.00')).toBeInTheDocument()
    expect(screen.getByText('Spend by category')).toBeInTheDocument()
    expect(screen.getByText('Spend by month')).toBeInTheDocument()

    const url = String(fetchMock.mock.calls[0][0])
    expect(url).toBe('/api/analytics/summary')
  })

  it('shows an error state with a retry button that refetches', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ error: 'boom' }, 500))
      .mockResolvedValueOnce(jsonResponse(summary))
    render(<AnalyticsPage />)

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent(/could not load analytics/i)
    expect(alert).toHaveTextContent(/500/)

    await userEvent.click(screen.getByRole('button', { name: /retry/i }))

    expect(await screen.findByText('$1,234.56')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('shows an empty state when there are no expenses', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(emptySummary))
    render(<AnalyticsPage />)

    expect(await screen.findByText(/no expenses found/i)).toBeInTheDocument()
    expect(screen.queryByText('Spend by category')).not.toBeInTheDocument()
  })

  it('passes from/to filters as query params', async () => {
    fetchMock.mockResolvedValue(jsonResponse(summary))
    render(<AnalyticsPage />)
    await screen.findByText('$1,234.56')

    const fromInput = screen.getByLabelText('From')
    await userEvent.type(fromInput, '2026-01-01')

    await waitFor(() => {
      const lastCall = fetchMock.mock.calls.at(-1)
      expect(String(lastCall?.[0])).toBe('/api/analytics/summary?from=2026-01-01')
    })
  })
})
