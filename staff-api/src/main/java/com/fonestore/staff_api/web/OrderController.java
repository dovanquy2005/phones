package com.fonestore.staff_api.web;

import com.fonestore.staff_api.dto.OrderSummaryDTO;
import com.fonestore.staff_api.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service){ this.service = service; }

    @GetMapping
    public List<OrderSummaryDTO> recent(){ return service.recent(); }
}
