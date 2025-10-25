package com.fonestore.user_api.dto;

import java.math.BigDecimal;


public record PaymentDTO(
        Long paymentId,
        String method,
        BigDecimal amount,
        String status,
        String txnRef,
        java.time.LocalDateTime createdAt
) {}
