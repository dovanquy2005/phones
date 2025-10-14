package com.fonestore.user_api.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
    Long orderId,
    BigDecimal total,
    List<Line> items
) {
    public record Line(Long skuId, Integer qty, BigDecimal unitPrice) {}
}
