package com.fonestore.staff_api.service;

import com.fonestore.staff_api.dto.OrderSummaryDTO;
import com.fonestore.staff_api.repo.OrderQueryRepository;
import com.fonestore.staff_api.repo.proj.OrderSummaryRow;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    private final OrderQueryRepository repo;

    public OrderService(OrderQueryRepository repo) {
        this.repo = repo;
    }

    public List<OrderSummaryRow> listSummary() {
        return repo.findAllSummary();
    }

    public List<OrderSummaryDTO> recent() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recent'");
    }
}
