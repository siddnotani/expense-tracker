package com.tracker.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsSummaryResponse(
        BigDecimal totalSpend,
        SummaryStats stats,
        List<CategorySpend> byCategory,
        List<MonthlySpend> byMonth) {
}
