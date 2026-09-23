import { apiGet } from './client'
import type { AnalyticsSummaryResponse } from '../types/analytics'

export interface AnalyticsSummaryParams {
  from?: string
  to?: string
}

export function getAnalyticsSummary(
  params?: AnalyticsSummaryParams,
): Promise<AnalyticsSummaryResponse> {
  return apiGet<AnalyticsSummaryResponse>('/api/analytics/summary', {
    from: params?.from,
    to: params?.to,
  })
}
