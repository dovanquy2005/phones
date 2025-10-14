package com.fonestore.staff_api.dto.user;

public record TwoFAResponse(
        Long userId,
        String secret,
        String otpauthUri
) {}
