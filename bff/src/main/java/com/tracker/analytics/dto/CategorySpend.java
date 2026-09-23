package com.tracker.analytics.dto;

import java.math.BigDecimal;

public record CategorySpend(String category, String displayName, BigDecimal total, BigDecimal percentage) {
}
