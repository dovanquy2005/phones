package com.fonestore.user_api.service;

import com.fonestore.user_api.dto.ProductDetailDTO;
import com.fonestore.user_api.dto.ProductListDTO;
import com.fonestore.user_api.repository.UserProductRepository;

import com.fonestore.staff_api.entity.Product;
import com.fonestore.staff_api.entity.ProductImage;
import com.fonestore.staff_api.entity.ProductVariant;
import com.fonestore.staff_api.repository.ProductImageRepository;
import com.fonestore.staff_api.repository.ProductVariantRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service("userProductService")
@RequiredArgsConstructor
public class ProductService {

    private static final String PUB_PREFIX = "/staff-frontend/assets/";

    private final UserProductRepository repo;
    private final ProductImageRepository imageRepo;
    private final ProductVariantRepository variantRepo;   // ⬅️ thêm

    private String toPublicUrl(String filePath) {
        if (filePath == null || filePath.isBlank()) return null;
        String p = filePath.replace("\\", "/");
        if (p.startsWith("http://") || p.startsWith("https://")) return p;
        if (p.startsWith(PUB_PREFIX)) return p;
        if (p.startsWith(PUB_PREFIX.substring(1))) return "/" + p;
        if (p.startsWith("/products/")) return PUB_PREFIX + p.substring(1);
        if (p.startsWith("products/"))  return PUB_PREFIX + p;
        if (p.startsWith("/")) return p;
        return PUB_PREFIX + p;
    }

    private String getCover(Long productId) {
        return imageRepo.findByProductIdOrderBySortOrderAsc(productId).stream()
                .findFirst()
                .map(ProductImage::getFilePath)
                .map(this::toPublicUrl)
                .orElse(null);
    }

    private Long getMinPrice(Long productId) {
        return variantRepo.findByProductIdOrderByIdAsc(productId).stream()
                .map(ProductVariant::getListPrice)
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .orElse(null);
    }

    public java.util.List<ProductListDTO> getAllActive() {
        return repo.findByIsActiveTrue().stream()
                .map(p -> new ProductListDTO(
                        p.getId(),
                        p.getName(),
                        p.getSlug(),
                        p.getDescription(),
                        p.getIsActive(),
                        p.getQuantity(),
                        getCover(p.getId()),
                        getMinPrice(p.getId())              // ⬅️ trả minPrice
                ))
                .toList();
    }

    public ProductDetailDTO getById(Long id) {
        Product p = repo.findById(id).orElseThrow();
        return new ProductDetailDTO(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.getSpecsJson(),
                p.getIsActive(),
                p.getQuantity(),
                getCover(p.getId()),
                getMinPrice(p.getId())                  // ⬅️ trả minPrice
        );
    }
}
