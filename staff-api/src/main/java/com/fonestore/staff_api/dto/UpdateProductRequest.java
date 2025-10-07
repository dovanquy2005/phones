package com.fonestore.staff_api.dto;

public record UpdateProductRequest(
    Long brandId,
    Long catId,
    String name,
    String slug,
    String description,
    String specsJson,
    Integer warrantyMonths,
    Boolean active
) {}
