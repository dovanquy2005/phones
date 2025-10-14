package com.fonestore.staff_api.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // bỏ qua key lạ trong JSON
public record UserCreateRequest(
        String email,
        String password,
        String fullName,
        String phone,
        String dob,      // yyyy-MM-dd
        String gender,
        String role
) {}
