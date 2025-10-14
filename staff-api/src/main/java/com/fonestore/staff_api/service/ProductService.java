package com.fonestore.staff_api.service;

import com.fonestore.staff_api.dto.*;
import com.fonestore.staff_api.entity.*;
import com.fonestore.staff_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("staffProductService")
@RequiredArgsConstructor
public class ProductService {

    private final StaffProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;

    /* ===== Helpers ===== */

    private String genSkuCode(Product p, String color, String capacity) {
        String base = Optional.ofNullable(p.getSlug())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> p.getName().replaceAll("\\s+", "-").toLowerCase());
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

        // Tăng sort_order tất cả ảnh hiện có lên +1
        for (ProductImage img : images) {
            img.setSortOrder(Optional.ofNullable(img.getSortOrder()).orElse(0) + 1);
        }
        if (!images.isEmpty()) {
            imageRepository.saveAll(images);
        }

        // Tìm xem path đã tồn tại chưa
        Optional<ProductImage> existed = images.stream()
                .filter(i -> path.equalsIgnoreCase(i.getFilePath()))
                .findFirst();

        ProductImage cover = existed.orElseGet(ProductImage::new);
        cover.setProductId(productId);
        cover.setFilePath(path);
        cover.setSortOrder(0);
        imageRepository.save(cover);
    }

    /* ===== Queries ===== */

    /** Danh sách sản phẩm: minPrice lấy theo biến thể, ảnh bìa lấy ảnh sort_order nhỏ nhất */
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

    /** Chi tiết sản phẩm: variants sort theo sku_id ASC, ảnh sort theo sort_order ASC */
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

    /* ===== Commands ===== */

    /** Tạo mới sản phẩm */
    @Transactional
    public ProductDetailDTO create(CreateProductRequest r) {
        Product p = new Product();
        p.setName(r.getName());
        p.setSlug(r.getSlug());
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

        if (r.getName() != null)        p.setName(r.getName());
        if (r.getSlug() != null)        p.setSlug(r.getSlug());
        if (r.getBrandId() != null)     p.setBrandId(r.getBrandId());
        if (r.getDescription() != null) p.setDescription(r.getDescription());
        if (r.getSpecsJson() != null)   p.setSpecsJson(r.getSpecsJson());
        if (r.getIsActive() != null)    p.setIsActive(r.getIsActive());
        if (r.getQuantity() != null)    p.setQuantity(Math.max(0, r.getQuantity()));
        productRepository.save(p);

        /* SỬA biến thể chính (sku_id nhỏ nhất). Nếu chưa có, tạo mới */
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
                // chưa có biến thể nào → tạo một cái
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

        /* Ảnh cover: đặt ảnh vừa chọn lên đầu */
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
