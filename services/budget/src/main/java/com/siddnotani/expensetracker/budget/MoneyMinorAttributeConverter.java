package com.siddnotani.expensetracker.budget;

import com.siddnotani.expensetracker.shared.money.Money;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter
public class MoneyMinorAttributeConverter implements AttributeConverter<BigDecimal, Long> {
    @Override
    public Long convertToDatabaseColumn(BigDecimal attribute) {
        return attribute == null ? null : Money.toMinorUnits(attribute);
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long dbData) {
        return dbData == null ? null : Money.fromMinorUnits(dbData);
    }
}
