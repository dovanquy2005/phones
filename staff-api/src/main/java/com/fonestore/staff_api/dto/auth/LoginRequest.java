package com.fonestore.staff_api.dto.auth;

public record LoginRequest(
        String email,
        String password
) {}
