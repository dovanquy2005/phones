package com.fonestore.user_api.dto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.*;

public record CreateOrderRequest(
    @NotNull Long userId,
    @NotBlank String status,
    @NotNull BigDecimal subtotal,
    @NotNull BigDecimal discount,
    @NotNull BigDecimal shippingFee,
    @NotNull BigDecimal total,
    @NotBlank String addressSnapshot,
    String note,
    @NotEmpty List<Item> items
) {
    public record Item(
        @NotNull Long skuId,
        @NotNull Integer qty,
        @NotNull BigDecimal unitPrice
    ) {}
}
