package com.fonestore.staff_api.dto;

import java.time.OffsetDateTime;

public record OrderSummaryDTO(
        Long orderId,
        String channel,
        String status,
        Long total,
        OffsetDateTime createdAt,
        String storeName,
        String customerName,
        String paymentStatus,
        Integer lines,
        Integer totalQty
) {}
