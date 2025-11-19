package com.fonestore.staff_api.dto.user;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String dob,
        String gender,
        String role,
        boolean twofaEnabled,
        String address
) {}
