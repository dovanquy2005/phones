package com.fonestore.staff_api.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerSummaryDTO(
        Long id,
        String name,
        String phone,
        BigDecimal totalSpent,
        Integer ordersCount,
        String lastOrderCode,
        LocalDateTime lastOrderAt
) { }
