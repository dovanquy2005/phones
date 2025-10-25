package com.fonestore.staff_api.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailDTO(
        Long id,
        String code,
        String status,
        String customerName,
        String customerPhone,
        String addressSnapshot,
        List<OrderItemDTO> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shippingFee,
        BigDecimal total,
        String note,
        Instant createdAt,
        Instant updatedAt
) {}
