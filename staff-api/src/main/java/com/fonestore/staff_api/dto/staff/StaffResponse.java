// dto/staff/StaffResponse.java
package com.fonestore.staff_api.dto.staff;

import java.time.LocalDateTime;

public record StaffResponse(
    Long staffId,
    String email,
    String fullName,
    String role,
    String position,
    String phone,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
