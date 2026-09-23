package com.tracker.analytics.dto;

import java.math.BigDecimal;

public record MonthlySpend(String month, BigDecimal total) {
}
