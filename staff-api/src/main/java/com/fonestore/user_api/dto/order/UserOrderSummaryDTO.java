package com.fonestore.user_api.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderSummaryDTO {
    private Long orderId;
    private String status;
    private Instant createdAt;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private BigDecimal total;
    private long itemCount;
    private String shortAddress; // optional
}
