export interface AnalyticsStats {
  count: number
  average: number
  min: number
  max: number
}

export interface CategoryBreakdown {
  category: string
  displayName: string
  total: number
  percentage: number
}

export interface MonthBreakdown {
  /** Formatted as YYYY-MM */
  month: string
  total: number
}

export interface AnalyticsSummaryResponse {
  totalSpend: number
  stats: AnalyticsStats
  byCategory: CategoryBreakdown[]
  byMonth: MonthBreakdown[]
}
