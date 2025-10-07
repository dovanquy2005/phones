package com.fonestore.staff_api.dto;

import java.math.BigDecimal;

public record CreateVariantRequest(
    String skuCode,
    String color,
    String capacity,
    BigDecimal listPrice,
    Boolean active
) {}
