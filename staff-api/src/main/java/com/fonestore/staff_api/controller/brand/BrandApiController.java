package com.fonestore.staff_api.controller.brand;

import com.fonestore.staff_api.dto.brand.BrandDTO;
import com.fonestore.staff_api.dto.brand.CreateBrandRequest;
import com.fonestore.staff_api.dto.brand.UpdateBrandRequest;
import com.fonestore.staff_api.service.brand.BrandService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandApiController {

    private final BrandService brandService;

    // GET /api/brands
    @GetMapping
    public ResponseEntity<List<BrandDTO>> list() {
        return ResponseEntity.ok(brandService.listAll());
    }

    // POST /api/brands
    @PostMapping
    public ResponseEntity<BrandDTO> create(@RequestBody CreateBrandRequest req) {
        return ResponseEntity.ok(brandService.create(req));
    }

    // PUT /api/brands/{id}
    @PutMapping("/{id}")
    public ResponseEntity<BrandDTO> update(@PathVariable Long id,
                                           @RequestBody UpdateBrandRequest req) {
        return ResponseEntity.ok(brandService.update(id, req));
    }

    // DELETE /api/brands/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
