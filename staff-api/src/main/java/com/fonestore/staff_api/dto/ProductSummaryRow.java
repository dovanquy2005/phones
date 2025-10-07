package com.fonestore.staff_api.dto;

import java.math.BigDecimal;

public interface ProductSummaryRow {
    Long getProductId();
    String getProductName();
    String getBrandName();
    String getCategoryName();
    String getSampleSku();
    Integer getQtyInStock();
    Integer getQtyReserved();
    Integer getQtySold();
    BigDecimal getMinPrice();
}
