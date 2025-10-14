package com.fonestore.staff_api.dto.user;

public record PasswordUpdateRequest(
        String oldPassword,
        String newPassword
) {}
