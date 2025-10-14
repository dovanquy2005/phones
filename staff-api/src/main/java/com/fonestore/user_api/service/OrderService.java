package com.fonestore.user_api.service;

import com.fonestore.user_api.dto.CreateOrderRequest;
import com.fonestore.user_api.dto.OrderResponse;
import com.fonestore.user_api.entity.Order;
import com.fonestore.user_api.entity.OrderItem;
import com.fonestore.user_api.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service("userOrderService")                 // ✅ tên bean trùng với Qualifier
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;

    @Transactional
    public OrderResponse create(CreateOrderRequest req) {
        Order o = new Order();
        o.setUserId(req.userId());
        o.setStatus(req.status());
        o.setSubtotal(req.subtotal());
        o.setDiscount(req.discount());
        o.setShippingFee(req.shippingFee());
        o.setTotal(req.total());
        o.setAddressSnapshot(req.addressSnapshot());
        o.setNote(req.note());

        List<OrderResponse.Line> lines = new ArrayList<>();
        for (CreateOrderRequest.Item item : req.items()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(o);
            oi.setSkuId(item.skuId());
            oi.setQuantity(item.qty());
            oi.setUnitPrice(item.unitPrice());
            o.getItems().add(oi);
            lines.add(new OrderResponse.Line(item.skuId(), item.qty(), item.unitPrice()));
        }

        orderRepo.save(o);
        return new OrderResponse(o.getId(), o.getTotal(), lines);
    }
}
