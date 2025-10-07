package com.fonestore.staff_api.repo.proj;

import java.time.LocalDateTime;

public interface OrderSummaryRow {
    Long getOrderId();
    Long getUserId();
    Long getTotalAmt();
    String getStatus();
    LocalDateTime getCreatedAt();
}
