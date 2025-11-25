// staff_api/repository/order/view/OrderItemDetailView.java
package com.fonestore.staff_api.repository.order.view;

import java.math.BigDecimal;

public interface OrderItemDetailView {
    Long getOrderItemId();
    Long getOrderId();
    Integer getQty();
    BigDecimal getUnitPrice();
    String getSkuCode();
    String getProductName();
    String getImageUrl();
    String getVariantInfo();
    String getBrandName();
    
}   
