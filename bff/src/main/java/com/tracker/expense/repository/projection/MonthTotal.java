package com.tracker.expense.repository.projection;

import java.math.BigDecimal;

public interface MonthTotal {

    int getYear();

    int getMonth();

    BigDecimal getTotal();
}
