package com.tracker.expense.repository.projection;

import java.math.BigDecimal;

public interface CategoryTotal {

    String getCategory();

    String getDisplayName();

    BigDecimal getTotal();
}
