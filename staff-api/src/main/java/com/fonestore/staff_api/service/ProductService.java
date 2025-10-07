package com.fonestore.staff_api.service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import com.fonestore.staff_api.dto.AddStockRequest;
import com.fonestore.staff_api.dto.CreateVariantRequest;
import com.fonestore.staff_api.dto.ProductDetailDTO;
import com.fonestore.staff_api.dto.ProductListDTO;
import com.fonestore.staff_api.entity.Product;
import com.fonestore.staff_api.entity.StockImei;
import com.fonestore.staff_api.entity.Variant;
import com.fonestore.staff_api.repository.ProductListRepository;
import com.fonestore.staff_api.repository.ProductRepository;
import com.fonestore.staff_api.repository.StockImeiRepository;
import com.fonestore.staff_api.repository.VariantRepository;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final VariantRepository variantRepo;
    private final ProductListRepository productListRepo;
    private final StockImeiRepository stockImeiRepo;

    public ProductService(ProductRepository productRepo,
                          VariantRepository variantRepo,
                          ProductListRepository productListRepo,
                          StockImeiRepository stockImeiRepo) {
        this.productRepo = productRepo;
        this.variantRepo = variantRepo;
        this.productListRepo = productListRepo;
        this.stockImeiRepo = stockImeiRepo;
    }

    // -------- LIST PAGE --------
    @Transactional(readOnly = true)
    public Page<ProductListDTO> page(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductListRepository.Row> p = productListRepo.page(pageable);
        return p.map(r -> new ProductListDTO(
            r.getId(),
            r.getName(),
            r.getBrand(),
            r.getCategory(),
            r.getSampleSku(),
            r.getMinPrice(),
            Optional.ofNullable(r.getQtyInStock()).orElse(0),
            Optional.ofNullable(r.getQtyReserved()).orElse(0),
            Optional.ofNullable(r.getQtySold()).orElse(0),
            r.getStatus() != null && r.getStatus() == 1
        ));
    }

    // -------- DETAIL --------
    @Transactional(readOnly = true)
    public ProductDetailDTO get(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));

        BigDecimal minPrice = variantRepo.findMinVariantPrice(id);

        List<Variant> variants = variantRepo.findByProduct_ProductId(id);
        List<ProductDetailDTO.VariantDTO> variantDTOs = variants.stream()
                .map(v -> new ProductDetailDTO.VariantDTO(
                        v.getSkuId(), v.getSkuCode(), v.getColor(), v.getCapacity(), v.getListPrice(), v.getIsActive()
                )).toList();

        return new ProductDetailDTO(
                p.getProductId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.getWarrantyMonths(),
                Boolean.TRUE.equals(p.getIsActive()),
                minPrice,
                variantDTOs
        );
    }

    // -------- CREATE / UPDATE / DELETE --------
    // Lưu ý: chỉ map vào cột có thật; bỏ mọi field tổng hợp (minPrice, qty*, brand name …)
    @Transactional
    public ProductDetailDTO create(com.fonestore.staff_api.dto.CreateProductRequest req) {
        Product p = new Product();
        p.setBrandId(req.brandId());
        p.setCatId(req.catId());
        p.setName(req.name());
        p.setSlug(req.slug());
        p.setDescription(req.description());
        p.setSpecsJson(req.specsJson());
        p.setWarrantyMonths(req.warrantyMonths());
        p.setIsActive(Boolean.TRUE.equals(req.active()));
        p = productRepo.save(p);
        return get(p.getProductId());
    }

    @Transactional
    public ProductDetailDTO update(Long id, com.fonestore.staff_api.dto.UpdateProductRequest req) {
        Product p = productRepo.findById(id).orElseThrow();
        if (req.brandId() != null) p.setBrandId(req.brandId());
        if (req.catId() != null) p.setCatId(req.catId());
        if (req.name() != null) p.setName(req.name());
        if (req.slug() != null) p.setSlug(req.slug());
        if (req.description() != null) p.setDescription(req.description());
        if (req.specsJson() != null) p.setSpecsJson(req.specsJson());
        if (req.warrantyMonths() != null) p.setWarrantyMonths(req.warrantyMonths());
        if (req.active() != null) p.setIsActive(req.active());
        productRepo.save(p);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        productRepo.deleteById(id);
    }
    


    @org.springframework.transaction.annotation.Transactional
    public com.fonestore.staff_api.dto.ProductDetailDTO addVariant(Long productId, CreateVariantRequest req) {
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));

        var v = new Variant();
        v.setProduct(product);
        v.setSkuCode(req.skuCode());
        v.setColor(req.color());
        v.setCapacity(req.capacity());
        v.setListPrice(req.listPrice());
        v.setIsActive(Boolean.TRUE.equals(req.active()));
        variantRepo.save(v);

        // Trả về detail sau khi thêm để FE cập nhật ngay minPrice/variants
        return get(productId);
    }



    @Transactional
    public void stockIn(Long skuId, AddStockRequest req) {
        var v = variantRepo.findById(skuId).orElseThrow();
        for (String code : req.imeis()) {
            var s = new StockImei();
            s.setVariant(v);
            s.setImei(code);
            s.setStatus("in_stock");
            stockImeiRepo.save(s);
        }
    }

}