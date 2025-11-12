package com.fonestore.staff_api.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueDTO(LocalDate date, BigDecimal revenue) {}
