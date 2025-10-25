package com.fonestore.staff_api.controller.product;

import com.fonestore.staff_api.dto.product.CreateProductRequest;
import com.fonestore.staff_api.dto.product.ProductDetailDTO;
import com.fonestore.staff_api.dto.product.ProductListDTO;
import com.fonestore.staff_api.dto.product.UpdateProductRequest;
import com.fonestore.staff_api.service.product.ProductService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductListDTO>> list() {
        return ResponseEntity.ok(productService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailDTO> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.getDetail(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ProductDetailDTO> create(@RequestBody CreateProductRequest req) {
        return ResponseEntity.ok(productService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDetailDTO> update(@PathVariable Long id,
                                                   @RequestBody UpdateProductRequest req) {
        // KHÔNG cần set productId vào req; service đã nhận id riêng
        return ResponseEntity.ok(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
