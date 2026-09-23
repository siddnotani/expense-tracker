package com.tracker.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tracker.analytics.dto.AnalyticsSummaryResponse;
import com.tracker.analytics.dto.CategorySpend;
import com.tracker.analytics.dto.MonthlySpend;
import com.tracker.expense.repository.ExpenseRepository;
import com.tracker.expense.repository.projection.CategoryTotal;
import com.tracker.expense.repository.projection.MonthTotal;
import com.tracker.expense.repository.projection.SpendStats;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private AnalyticsService service;

    @Test
    void returnsZerosAndEmptyListsWhenNoExpenses() {
        when(expenseRepository.stats(null, null)).thenReturn(stats(0, "0", null, null));

        AnalyticsSummaryResponse response = service.summary(null, null);

        assertThat(response.totalSpend()).isEqualByComparingTo("0.00");
        assertThat(response.totalSpend().scale()).isEqualTo(2);
        assertThat(response.stats().count()).isZero();
        assertThat(response.stats().average()).isEqualByComparingTo("0.00");
        assertThat(response.stats().min()).isEqualByComparingTo("0.00");
        assertThat(response.stats().max()).isEqualByComparingTo("0.00");
        assertThat(response.byCategory()).isEmpty();
        assertThat(response.byMonth()).isEmpty();
        verify(expenseRepository, never()).sumByCategory(any(), any());
        verify(expenseRepository, never()).sumByMonth(any(), any());
    }

    @Test
    void aggregatesAndRoundsValues() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);
        when(expenseRepository.stats(from, to)).thenReturn(stats(3, "100.00", "10.005", "60.00"));
        when(expenseRepository.sumByCategory(from, to)).thenReturn(List.of(
                categoryTotal("FOOD", "Food", "66.666"),
                categoryTotal("TRANSPORT", "Transport", "33.334")));
        when(expenseRepository.sumByMonth(from, to)).thenReturn(List.of(
                monthTotal(2026, 3, "40.00"),
                monthTotal(2026, 4, "60.00")));

        AnalyticsSummaryResponse response = service.summary(from, to);

        assertThat(response.totalSpend()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.stats().count()).isEqualTo(3);
        assertThat(response.stats().average()).isEqualTo(new BigDecimal("33.33"));
        assertThat(response.stats().min()).isEqualTo(new BigDecimal("10.01"));
        assertThat(response.stats().max()).isEqualTo(new BigDecimal("60.00"));

        assertThat(response.byCategory()).containsExactly(
                new CategorySpend("FOOD", "Food", new BigDecimal("66.67"), new BigDecimal("66.7")),
                new CategorySpend("TRANSPORT", "Transport", new BigDecimal("33.33"), new BigDecimal("33.3")));
        assertThat(response.byMonth()).containsExactly(
                new MonthlySpend("2026-03", new BigDecimal("40.00")),
                new MonthlySpend("2026-04", new BigDecimal("60.00")));
    }

    private static SpendStats stats(long count, String total, String min, String max) {
        return new SpendStats() {
            @Override
            public long getCount() {
                return count;
            }

            @Override
            public BigDecimal getTotal() {
                return new BigDecimal(total);
            }

            @Override
            public BigDecimal getMin() {
                return min == null ? null : new BigDecimal(min);
            }

            @Override
            public BigDecimal getMax() {
                return max == null ? null : new BigDecimal(max);
            }
        };
    }

    private static CategoryTotal categoryTotal(String category, String displayName, String total) {
        return new CategoryTotal() {
            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public String getDisplayName() {
                return displayName;
            }

            @Override
            public BigDecimal getTotal() {
                return new BigDecimal(total);
            }
        };
    }

    private static MonthTotal monthTotal(int year, int month, String total) {
        return new MonthTotal() {
            @Override
            public int getYear() {
                return year;
            }

            @Override
            public int getMonth() {
                return month;
            }

            @Override
            public BigDecimal getTotal() {
                return new BigDecimal(total);
            }
        };
    }
}
