package com.tracker.expense.repository.projection;

import java.math.BigDecimal;

public interface SpendStats {

    long getCount();

    BigDecimal getTotal();

    BigDecimal getMin();

    BigDecimal getMax();
}
