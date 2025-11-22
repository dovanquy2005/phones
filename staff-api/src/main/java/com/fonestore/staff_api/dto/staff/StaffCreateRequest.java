// dto/staff/StaffCreateRequest.java
package com.fonestore.staff_api.dto.staff;

import jakarta.validation.constraints.*;

public record StaffCreateRequest(
    @Email @NotBlank String email,
    @NotBlank String fullName,
    @NotBlank String role,             // "manager" | "staff"
    @Size(min = 6) String password,    // plaintext -> BE hash
    String phone,
    Boolean isActive,                  // null -> mặc định true
    Boolean sendInvite                 // tuỳ bạn dùng
) {}
