package com.fonestore.user_api.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record MergeCartRequest(
        @NotEmpty List<Item> items
) {
    public record Item(
        @NotNull Long skuId,
        String name,
        BigDecimal price,
        String imageUrl,
        @NotNull @Min(1) Integer qty
    ) {}
}