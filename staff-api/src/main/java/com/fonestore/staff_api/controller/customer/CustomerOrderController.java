package com.fonestore.staff_api.controller.customer;

import com.fonestore.user_api.entity.Order;
import com.fonestore.user_api.repository.order.OrderRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderRepository orderRepo;

    @GetMapping("/{id}/orders")
    public Page<Map<String, Object>> ordersOfCustomer(
            @PathVariable("id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var pageable = PageRequest.of(page, Math.max(1, size));
        Page<Order> p = orderRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return p.map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            Long oid = o.getId(); // nếu entity là orderId -> dùng o.getOrderId()
            m.put("orderId", oid);
            m.put("code", String.format("OD-%06d", oid));
            m.put("total", o.getTotal());
            m.put("status", o.getStatus());
            m.put("createdAt", o.getCreatedAt());
            return m;
        });
    }
}
