package com.fonestore.user_api.dto.cart;

import jakarta.validation.constraints.NotNull;

public record UpdateItemRequest(
        @NotNull Long itemId,
        @NotNull Integer qty // 0 => remove
) {}