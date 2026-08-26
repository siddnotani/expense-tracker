package com.siddnotani.expensetracker.bff;

import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class BffPeriodParser {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM");

    public YearMonth parseOrCurrent(String value) {
        if (value == null || value.isBlank()) {
            return YearMonth.now();
        }
        if (!value.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("period must use YYYY-MM format");
        }
        try {
            return YearMonth.parse(value, FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("period must use YYYY-MM format", exception);
        }
    }
}
