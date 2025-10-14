package com.fonestore.staff_api.dto.user;

public record UserUpdateRequest(
        String fullName,
        String phone,
        String dob,     // yyyy-MM-dd
        String gender,
        String role
) {}
