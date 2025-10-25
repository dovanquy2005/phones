package com.fonestore.staff_api.service.product;


import com.fonestore.staff_api.dto.product.CreateProductRequest;
import com.fonestore.staff_api.dto.product.ProductDetailDTO;
import com.fonestore.staff_api.dto.product.ProductImageDTO;
import com.fonestore.staff_api.dto.product.ProductListDTO;
import com.fonestore.staff_api.dto.product.ProductVariantDTO;
import com.fonestore.staff_api.dto.product.UpdateProductRequest;
import com.fonestore.staff_api.entity.*;

import com.fonestore.staff_api.repository.brand.BrandRepository;
import com.fonestore.staff_api.repository.product.ProductImageRepository;
import com.fonestore.staff_api.repository.product.ProductVariantRepository;
import com.fonestore.staff_api.repository.product.StaffProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service("staffProductService")
@RequiredArgsConstructor
public class ProductService {

    private final StaffProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;

    /* ================= Helpers ================= */

    private static String slugify(String s) {
        if (s == null) return "";
        String noDiac = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noDiac.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
    }

    /** Tạo slug duy nhất. Nếu đã tồn tại, thêm -2, -3, ... */
    private String ensureUniqueSlug(String base, Long excludeId) {
        String slug = base;
        int k = 2;
        if (excludeId == null) {
            while (productRepository.existsBySlug(slug)) {
                slug = base + "-" + k++;
                if (k > 500) break;
            }
        } else {
            while (productRepository.existsBySlugAndIdNot(slug, excludeId)) {
                slug = base + "-" + k++;
                if (k > 500) break;
            }
        }
        return slug;
    }

    private String genSkuCode(Product p, String color, String capacity) {
        String base = Optional.ofNullable(p.getSlug())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> slugify(p.getName()));
        String cc = (Optional.ofNullable(color).orElse("-") + "-" +
                Optional.ofNullable(capacity).orElse("-"))
                .replaceAll("\\s+", "-").toLowerCase();
        String tail = String.valueOf(System.currentTimeMillis() % 10000);
        return (base + "-" + cc + "-" + tail).replaceAll("[^a-z0-9-]", "");
    }

    /** Đặt ảnh bìa: path trở thành sort_order=0, các ảnh khác +1 */
    private void setCoverImage(Long productId, String path) {
        if (path == null || path.isBlank()) return;

        List<ProductImage> images = imageRepository.findByProductIdOrderBySortOrderAsc(productId);

        for (ProductImage img : images) {
            img.setSortOrder(Optional.ofNullable(img.getSortOrder()).orElse(0) + 1);
        }
        if (!images.isEmpty()) {
            imageRepository.saveAll(images);
        }

        Optional<ProductImage> existed = images.stream()
                .filter(i -> path.equalsIgnoreCase(i.getFilePath()))
                .findFirst();

        ProductImage cover = existed.orElseGet(ProductImage::new);
        cover.setProductId(productId);
        cover.setFilePath(path);
        cover.setSortOrder(0);
        imageRepository.save(cover);
    }

    /* ================= Queries ================= */

    public List<ProductListDTO> listAll() {
        List<Product> products = productRepository.findAll();

        Map<Long, List<ProductVariant>> variantsByProduct = variantRepository.findAll()
                .stream().collect(Collectors.groupingBy(ProductVariant::getProductId));

        Map<Long, List<ProductImage>> imagesByProduct = imageRepository.findAll()
                .stream().collect(Collectors.groupingBy(ProductImage::getProductId));

        Map<Long, String> brandNameById = brandRepository.findAll()
                .stream().collect(Collectors.toMap(Brand::getId, Brand::getName));

        List<ProductListDTO> list = new ArrayList<>();
        for (Product p : products) {
            long minPrice = variantsByProduct.getOrDefault(p.getId(), Collections.emptyList())
                    .stream()
                    .map(v -> Optional.ofNullable(v.getListPrice()).orElse(0L))
                    .min(Long::compareTo).orElse(0L);

            String firstImage = imagesByProduct.getOrDefault(p.getId(), Collections.emptyList())
                    .stream()
                    .sorted(Comparator
                            .comparing((ProductImage img) -> Optional.ofNullable(img.getSortOrder()).orElse(0))
                            .thenComparing(ProductImage::getId))
                    .map(ProductImage::getFilePath)
                    .findFirst().orElse(null);

            list.add(new ProductListDTO(
                    p.getId(),
                    p.getName(),
                    brandNameById.getOrDefault(p.getBrandId(), ""),
                    minPrice,
                    Optional.ofNullable(p.getIsActive()).orElse(Boolean.TRUE),
                    firstImage,
                    Optional.ofNullable(p.getQuantity()).orElse(0)
            ));
        }
        return list;
    }

    public ProductDetailDTO getDetail(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));

        List<ProductVariantDTO> variants = variantRepository
                .findByProductIdOrderByIdAsc(id).stream()
                .map(v -> new ProductVariantDTO(v.getId(), v.getColor(), v.getCapacity(), v.getListPrice()))
                .toList();

        List<ProductImageDTO> images = imageRepository.findByProductIdOrderBySortOrderAsc(id).stream()
                .map(i -> new ProductImageDTO(i.getId(), i.getFilePath(), i.getSortOrder()))
                .toList();

        String coverImage = images.stream().findFirst().map(ProductImageDTO::getFilePath).orElse(null);

        return new ProductDetailDTO(
                p.getId(), p.getName(), p.getSlug(), p.getBrandId(), p.getDescription(),
                p.getSpecsJson(), p.getIsActive(), coverImage, p.getQuantity(), variants, images
        );
    }

    /* ================= Commands ================= */

    /** Tạo mới sản phẩm */
    @Transactional
    public ProductDetailDTO create(CreateProductRequest r) {
        // Validate brand
        if (r.getBrandId() == null || !brandRepository.existsById(r.getBrandId())) {
            throw new NoSuchElementException("Brand not found");
        }

        // Slug chuẩn & duy nhất
        String baseSlug = (r.getSlug() != null && !r.getSlug().isBlank())
                ? slugify(r.getSlug())
                : slugify(r.getName());
        String finalSlug = ensureUniqueSlug(baseSlug, null);

        Product p = new Product();
        p.setName(r.getName());
        p.setSlug(finalSlug);
        p.setBrandId(r.getBrandId());
        p.setDescription(r.getDescription());
        p.setSpecsJson(Optional.ofNullable(r.getSpecsJson()).orElse("{}"));
        p.setIsActive(Optional.ofNullable(r.getIsActive()).orElse(Boolean.TRUE));
        p.setQuantity(Optional.ofNullable(r.getQuantity()).orElse(0));
        p = productRepository.save(p);

        // Biến thể đầu (nếu có)
        if (r.getFirstListPrice() != null) {
            ProductVariant v = new ProductVariant();
            v.setProductId(p.getId());
            v.setColor(r.getFirstColor());
            v.setCapacity(r.getFirstCapacity());
            v.setListPrice(r.getFirstListPrice());
            v.setIsActive(Boolean.TRUE);
            v.setSkuCode(genSkuCode(p, r.getFirstColor(), r.getFirstCapacity()));
            variantRepository.save(v);
        }

        // Ảnh cover (nếu có)
        if (r.getImagePath() != null && !r.getImagePath().isBlank()) {
            setCoverImage(p.getId(), r.getImagePath());
        }

        return getDetail(p.getId());
    }

    /** Cập nhật sản phẩm */
    @Transactional
    public ProductDetailDTO update(Long id, UpdateProductRequest r) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));

        // Brand
        if (r.getBrandId() != null) {
            if (!brandRepository.existsById(r.getBrandId())) {
                throw new NoSuchElementException("Brand not found");
            }
            p.setBrandId(r.getBrandId());
        }

        // Name & Slug:
        // - Nếu có slug trong request -> ưu tiên, nhưng vẫn slugify + ensure unique (exclude chính nó)
        // - Nếu không có slug nhưng có name mới -> slugify(name) + ensure unique (exclude chính nó)
        if (r.getSlug() != null && !r.getSlug().isBlank()) {
            String base = slugify(r.getSlug());
            String unique = ensureUniqueSlug(base, id);
            p.setSlug(unique);
        } else if (r.getName() != null && !r.getName().isBlank()) {
            String base = slugify(r.getName());
            // nếu base khác slug hiện tại mới cần check unique
            if (!base.equals(p.getSlug())) {
                String unique = ensureUniqueSlug(base, id);
                p.setSlug(unique);
            }
        }

        if (r.getName() != null)        p.setName(r.getName());
        if (r.getDescription() != null) p.setDescription(r.getDescription());
        if (r.getSpecsJson() != null)   p.setSpecsJson(r.getSpecsJson());
        if (r.getIsActive() != null)    p.setIsActive(r.getIsActive());
        if (r.getQuantity() != null)    p.setQuantity(Math.max(0, r.getQuantity()));
        productRepository.save(p); // UPDATE (đã có id)

        /* Biến thể chính (sku nhỏ nhất) — sửa hoặc tạo mới nếu có add* */
        boolean hasVariantFields =
                (r.getAddColor() != null && !r.getAddColor().isBlank()) ||
                (r.getAddCapacity() != null && !r.getAddCapacity().isBlank()) ||
                (r.getAddListPrice() != null);

        if (hasVariantFields) {
            Optional<ProductVariant> optMain = variantRepository.findFirstByProductIdOrderByIdAsc(p.getId());
            if (optMain.isPresent()) {
                ProductVariant v = optMain.get();
                if (r.getAddColor() != null)    v.setColor(r.getAddColor());
                if (r.getAddCapacity() != null) v.setCapacity(r.getAddCapacity());
                if (r.getAddListPrice() != null) v.setListPrice(r.getAddListPrice());
                if (v.getSkuCode() == null || v.getSkuCode().isBlank()) {
                    v.setSkuCode(genSkuCode(p, v.getColor(), v.getCapacity()));
                }
                v.setIsActive(Boolean.TRUE);
                variantRepository.save(v);
            } else {
                ProductVariant v = new ProductVariant();
                v.setProductId(p.getId());
                v.setColor(r.getAddColor());
                v.setCapacity(r.getAddCapacity());
                v.setListPrice(Optional.ofNullable(r.getAddListPrice()).orElse(0L));
                v.setIsActive(Boolean.TRUE);
                v.setSkuCode(genSkuCode(p, r.getAddColor(), r.getAddCapacity()));
                variantRepository.save(v);
            }
        }

        /* Ảnh cover */
        if (r.getImagePath() != null && !r.getImagePath().isBlank()) {
            setCoverImage(p.getId(), r.getImagePath());
        }

        return getDetail(p.getId());
    }

    /** Xóa sản phẩm */
    @Transactional
    public void delete(Long id) {
        imageRepository.findByProductIdOrderBySortOrderAsc(id)
                .forEach(img -> imageRepository.deleteById(img.getId()));
        variantRepository.findByProductId(id)
                .forEach(v -> variantRepository.deleteById(v.getId()));
        productRepository.deleteById(id);
    }
}
