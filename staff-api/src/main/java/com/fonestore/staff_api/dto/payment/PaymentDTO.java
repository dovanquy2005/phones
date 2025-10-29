package com.fonestore.staff_api.dto.payment;

import com.fonestore.staff_api.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDTO(
        Long paymentId,
        Long orderId,
        String method,
        BigDecimal amount,
        PaymentStatus status,
        String txnRef,
        LocalDateTime createdAt
) {}
