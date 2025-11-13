package com.fonestore.user_api.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class UserOrderDetailDTO {
    private Long orderId;
    private String status;
    private Instant createdAt;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private BigDecimal total;
    private List<UserOrderItemSummaryDTO> items;
    
    // optional shipping/payment info
    private String paymentMethod;
    private String shippingAddress;
    private String note;
}
