package com.fonestore.user_api.service;

public interface StockGateway {
    /** Trả về on_hand (>=0) nếu biết, hoặc null nếu không có dữ liệu tồn kho. */
    Integer getOnHand(Long skuId);
}
