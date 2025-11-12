package com.fonestore.user_api.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartDTO(
        Long orderId,
        String status,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shippingFee,
        BigDecimal total,
        List<CartItemDTO> items
) {}