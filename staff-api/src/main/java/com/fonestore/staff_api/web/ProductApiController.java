package com.fonestore.staff_api.web;

import com.fonestore.staff_api.dto.ProductListDTO;
import com.fonestore.staff_api.dto.ProductDetailDTO;
import com.fonestore.staff_api.dto.CreateProductRequest;
import com.fonestore.staff_api.dto.CreateVariantRequest;
import com.fonestore.staff_api.dto.UpdateProductRequest;
import com.fonestore.staff_api.service.ProductService;

import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {
    private final ProductService service;
    public ProductApiController(ProductService service) { this.service = service; }

    @GetMapping
    public Page<ProductListDTO> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return service.page(page, size);
    }

    @GetMapping("/{id}")
    public ProductDetailDTO detail(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ProductDetailDTO> create(@RequestBody CreateProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ProductDetailDTO update(@PathVariable Long id, @RequestBody UpdateProductRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }


    //-------------
    @PostMapping("/{id}/variants")
    public ResponseEntity<ProductDetailDTO> addVariant(
            @PathVariable Long id,
            @RequestBody CreateVariantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addVariant(id, req));
    }
    
}