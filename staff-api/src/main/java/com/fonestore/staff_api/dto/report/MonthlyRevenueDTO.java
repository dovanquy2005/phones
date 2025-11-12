// com.fonestore.staff_api.dto.report.MonthlyRevenueDTO
package com.fonestore.staff_api.dto.report;
import java.math.BigDecimal;

public record MonthlyRevenueDTO(int month, BigDecimal revenue) {}
