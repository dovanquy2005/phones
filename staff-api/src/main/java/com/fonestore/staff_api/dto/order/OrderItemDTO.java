package com.fonestore.staff_api.dto.order;

import java.math.BigDecimal;

public record OrderItemDTO(
        Long orderItemId,
        Long skuId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
