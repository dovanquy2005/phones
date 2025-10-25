// staff_api/dto/order/OrderItemDetailDTO.java
package com.fonestore.staff_api.dto.order;

import com.fonestore.staff_api.repository.order.view.OrderItemDetailView;

import java.math.BigDecimal;

public record OrderItemDetailDTO(
        Long orderItemId,
        Long orderId,
        Integer qty,
        BigDecimal unitPrice,
        String skuCode,
        String productName
) {
    public static OrderItemDetailDTO from(OrderItemDetailView v) {
        return new OrderItemDetailDTO(
                v.getOrderItemId(),
                v.getOrderId(),
                v.getQty(),
                v.getUnitPrice(),
                v.getSkuCode(),
                v.getProductName()
        );
    }
}
