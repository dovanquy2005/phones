package com.fonestore.staff_api.dto.staff;

public record StaffCreateRequest(
        Long userId,
        String position,
        String phone,
        String note
) {}
