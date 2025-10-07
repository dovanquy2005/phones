package com.fonestore.staff_api.dto;

public record CategoryDTO(
    Long id, 
    String name, 
    Long parentId
) {}
