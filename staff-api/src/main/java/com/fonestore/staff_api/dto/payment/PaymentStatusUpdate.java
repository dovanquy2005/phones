package com.fonestore.staff_api.dto.payment;

public record PaymentStatusUpdate(
        String status // "PAID" | "UNPAID"
) {}
