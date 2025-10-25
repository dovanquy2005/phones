package com.fonestore.staff_api.controller.product;


import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/products")
@RequiredArgsConstructor // tạo constructor cho các field final
public class StaffProductController {

    @GetMapping
    public Object page() {
        return null;
    }
}
