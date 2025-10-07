// src/main/java/com/fonestore/staff_api/controller/ProductPageController.java
package com.fonestore.staff_api.controller;


import com.fonestore.staff_api.dto.CreateProductRequest;
import com.fonestore.staff_api.dto.ProductDetailDTO;
import com.fonestore.staff_api.dto.UpdateProductRequest;
import com.fonestore.staff_api.service.ProductService;


import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller; // dùng @Controller nếu render view, hoặc giữ @RestController nếu trả JSON
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/products")
public class ProductPageController {

    private final ProductService productService;

    public ProductPageController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ProductDetailDTO> create(@RequestBody CreateProductRequest req){
        return ResponseEntity.ok(productService.create(req));
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ProductDetailDTO> get(@PathVariable Long id){
        return ResponseEntity.ok(productService.get(id));
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ProductDetailDTO> update(@PathVariable Long id, @RequestBody UpdateProductRequest req){
        return ResponseEntity.ok(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable Long id){
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }


}
