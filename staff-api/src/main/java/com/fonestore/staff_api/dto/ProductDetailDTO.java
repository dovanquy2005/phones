package com.fonestore.staff_api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailDTO(
    Long id,
    String name,
    String slug,
    String description,
    Integer warrantyMonths,
    boolean active,
    BigDecimal minPrice,
    List<VariantDTO> variants
) {
    public record VariantDTO(
        Long skuId,
        String skuCode,
        String color,
        String capacity,
        BigDecimal price,
        Boolean active
    ){}
}
