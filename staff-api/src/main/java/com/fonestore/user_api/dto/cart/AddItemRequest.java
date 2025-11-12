package com.fonestore.user_api.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddItemRequest(
        @NotNull Long skuId,

        @NotNull @Min(1) Integer qty
) {}