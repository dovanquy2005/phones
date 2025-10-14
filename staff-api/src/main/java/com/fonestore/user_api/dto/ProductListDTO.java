package com.fonestore.user_api.dto;

public record ProductListDTO(
    Long id,
    String name,
    String slug,
    String description,
    Boolean isActive,
    Integer quantity,
    String imageUrl,
    Long minPrice                    // ⬅️ thêm
) {}
