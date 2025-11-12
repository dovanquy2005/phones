// src/main/java/com/fonestore/staff_api/entity/ProductVariant.java
package com.fonestore.staff_api.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.Setter;

@Entity
@Table(name = "product_variants")
@Getter @Setter
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // THÊM FIELD NÀY
    @Column(name = "sku_code", nullable = false, length = 100)
    private String skuCode;

    @Column(name = "color", length = 100)
    private String color;

    @Column(name = "capacity", length = 100)
    private String capacity;

    // kiểu Long hoặc BigDecimal tuỳ mapping trước đó; giữ nguyên bạn đang dùng
    @Column(name = "list_price", nullable = false)
    private Long listPrice;

    @Column(name = "is_active")
    private Boolean isActive;
}
