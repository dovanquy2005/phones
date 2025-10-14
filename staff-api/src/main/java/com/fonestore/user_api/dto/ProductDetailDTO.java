package com.fonestore.user_api.dto;

public record ProductDetailDTO(
    Long id,
    String name,
    String slug,
    String description,
    String specsJson,
    Boolean isActive,
    Integer quantity,
    String imageUrl,
    Long minPrice                    // ⬅️ thêm
) {}
