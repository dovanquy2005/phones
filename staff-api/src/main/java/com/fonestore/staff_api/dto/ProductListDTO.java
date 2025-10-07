package com.fonestore.staff_api.dto;

import java.math.BigDecimal;

public record ProductListDTO(
    Long id,
    String name,
    String brand,
    String category,
    String sampleSku,
    BigDecimal minPrice,
    Integer qtyInStock,
    Integer qtyReserved,
    Integer qtySold,
    boolean active
) {}
