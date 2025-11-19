package com.fonestore.staff_api.controller.customer;

import com.fonestore.staff_api.dto.customer.CustomerSummaryDTO;
import com.fonestore.staff_api.dto.user.UserResponse; // Import DTO
import com.fonestore.staff_api.service.customer.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerSummaryDTO> list(@RequestParam(value = "q", required = false) String q) {
        return service.list(q);
    }

    // --- Endpoint MỚI ---
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(service.getDetail(id));
    }
}