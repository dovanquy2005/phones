package com.fonestore.user_api.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fonestore.user_api.dto.PaymentDTO;
import com.fonestore.user_api.dto.ShipmentDTO;

public record PagedOrderResponse(
        Long orderId,
        String status,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shippingFee,
        BigDecimal total,
        Instant createdAt,
        String addressSnapshot,
        String note,
        List<Line> lines,
        List<PaymentDTO> payments,
        List<ShipmentDTO> shipments
) {
    public record Line(
            Long skuId,
            Integer qty,
            BigDecimal unitPrice,
            String productName
    ) {}
}

