package com.fonestore.staff_api.controller;

import com.fonestore.staff_api.service.ProductService;
import com.fonestore.staff_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/products")
@RequiredArgsConstructor // tạo constructor cho các field final
public class StaffProductController {

    @Qualifier("staffProductService")
    private final ProductService productService;

    @Qualifier("staffUserService")
    private final UserService userService;

    @GetMapping
    public Object page() {
        return null;
    }
}
