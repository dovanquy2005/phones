package com.fonestore.staff_api.dto.staff;

public record StaffResponse(
        Long staffId,
        Long userId,
        String email,
        String position,
        String phone,
        String note
) {}
