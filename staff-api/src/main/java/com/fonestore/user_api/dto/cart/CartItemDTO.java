package com.fonestore.user_api.dto.cart;

import java.math.BigDecimal;

public record CartItemDTO(
        Long id,
        Long skuId,
        Integer qty,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String name,
        String imagePath
) {}