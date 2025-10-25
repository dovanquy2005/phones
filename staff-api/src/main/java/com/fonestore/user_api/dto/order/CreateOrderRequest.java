package com.fonestore.user_api.dto.order;

import java.util.List;
import jakarta.validation.constraints.*;

// Minimal payload: BE will compute all money fields from skuId
public record CreateOrderRequest(
    @NotNull Long userId,
    @NotBlank String addressSnapshot,
    String note,
    @NotEmpty List<Item> items
) {
    public record Item(
        @NotNull Long skuId,
        @NotNull Integer qty
    ) {}
}
