package com.fonestore.staff_api.entity.converter;

import com.fonestore.staff_api.entity.enums.PaymentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus attribute) {
        if (attribute == null) return "pending";
        return switch (attribute) {
            case PAID -> "paid";
            default   -> "pending"; // UNPAID
        };
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return PaymentStatus.UNPAID;
        final String v = dbValue.trim().toLowerCase();
        return switch (v) {
            case "paid", "1", "done" -> PaymentStatus.PAID;
            default -> PaymentStatus.UNPAID; // "pending", "unpaid", null...
        };
    }
}

