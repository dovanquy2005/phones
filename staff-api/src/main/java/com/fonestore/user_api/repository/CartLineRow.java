// src/main/java/com/fonestore/user_api/repository/CartLineRow.java
package com.fonestore.user_api.repository;

import java.math.BigDecimal;

public interface CartLineRow {
    Long getId();
    Long getSkuId();
    Integer getQuantity();
    BigDecimal getUnitPrice();
    String getProductName();
    String getImagePath();
    String getColor();
    String getCapacity();
}
