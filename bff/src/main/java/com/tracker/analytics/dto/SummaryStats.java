package com.tracker.analytics.dto;

import java.math.BigDecimal;

public record SummaryStats(long count, BigDecimal average, BigDecimal min, BigDecimal max) {
}
