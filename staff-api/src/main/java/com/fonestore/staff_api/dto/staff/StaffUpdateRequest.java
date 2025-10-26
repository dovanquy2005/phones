// dto/staff/StaffUpdateRequest.java

package com.fonestore.staff_api.dto.staff;

public record StaffUpdateRequest(
    String email,
    String fullName,
    String role,         // "manager" | "staff"
    String password,     // nếu có -> hash
    String position,
    String phone,
    Boolean isActive
) {}
