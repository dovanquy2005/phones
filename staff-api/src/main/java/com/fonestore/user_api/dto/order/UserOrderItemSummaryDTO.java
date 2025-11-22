package com.fonestore.user_api.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class UserOrderItemSummaryDTO {
    private Long itemId;
    private Long skuId;
    private String productName;
    private String imagePath;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private String color;
    private String capacity;
}
