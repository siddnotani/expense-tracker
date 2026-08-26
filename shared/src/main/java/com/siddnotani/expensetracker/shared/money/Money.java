package com.siddnotani.expensetracker.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money() {
    }

    public static BigDecimal fromMinorUnits(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    public static long toMinorUnits(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact();
    }
}
