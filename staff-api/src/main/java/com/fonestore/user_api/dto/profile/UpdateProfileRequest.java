package com.fonestore.user_api.dto.profile;

import java.time.LocalDate;

public record UpdateProfileRequest(
    String fullName,
    String phone,
    LocalDate dob,
    String gender,
    String address
) {}