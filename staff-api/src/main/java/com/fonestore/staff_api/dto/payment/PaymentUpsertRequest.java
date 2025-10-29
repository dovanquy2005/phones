package com.fonestore.staff_api.dto.payment;

import java.math.BigDecimal;

public record PaymentUpsertRequest(
        String method,       // "cod" | "bank"
        BigDecimal amount,   // nếu null -> lấy theo order.total
        String txnRef        // optional
) {}
