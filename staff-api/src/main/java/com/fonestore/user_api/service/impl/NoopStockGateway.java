package com.fonestore.user_api.service.impl;

import com.fonestore.user_api.service.StockGateway;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class NoopStockGateway implements StockGateway {
    @Override
    public Integer getOnHand(Long skuId) {
        return null; // chưa tích hợp kho -> không kiểm tra
    }
}
