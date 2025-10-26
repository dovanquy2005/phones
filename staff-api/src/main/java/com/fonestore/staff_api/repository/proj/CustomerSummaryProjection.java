package com.fonestore.staff_api.repository.proj;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CustomerSummaryProjection {
    Long getId();
    String getName();
    String getPhone();
    BigDecimal getTotalSpent();
    Integer getOrdersCount();
    String getLastOrderCode();
    LocalDateTime getLastOrderAt();
}
