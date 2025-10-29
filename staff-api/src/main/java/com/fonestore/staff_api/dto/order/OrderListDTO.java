package com.fonestore.staff_api.dto.order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderListDTO(
        Long id,
        String code,             // nếu chưa dùng có thể để null
        String customerName,     // trích từ addressSnapshot
        String status,           // trạng thái ĐƠN: PENDING/SHIPPING/DELIVERED/CANCELED
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal total,
        Instant createdAt,
        String paymentStatus     // NEW: PAID/UNPAID
) {}
