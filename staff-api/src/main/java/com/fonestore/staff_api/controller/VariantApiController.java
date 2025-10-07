package com.fonestore.staff_api.controller;

import com.fonestore.staff_api.dto.AddStockRequest;
import com.fonestore.staff_api.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class VariantApiController {

    private final ProductService service;

    public VariantApiController(ProductService service) {
        this.service = service;
    }

    @PostMapping("/variants/{skuId}/stock-in")
    public ResponseEntity<Void> stockIn(@PathVariable Long skuId,
                                        @RequestBody AddStockRequest req) {
        service.stockIn(skuId, req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
