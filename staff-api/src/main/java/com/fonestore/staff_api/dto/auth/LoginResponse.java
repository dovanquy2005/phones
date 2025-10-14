package com.fonestore.staff_api.dto.auth;

public record LoginResponse(
        Long userId,
        String email,
        String role,
        boolean twofaEnabled,
        String token
) {}
