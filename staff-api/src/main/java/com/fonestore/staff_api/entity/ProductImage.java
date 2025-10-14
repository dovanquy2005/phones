package com.fonestore.staff_api.entity;
import jakarta.persistence.*; 
import lombok.Getter; import lombok.Setter;
@Entity @Table(name = "product_images") @Getter @Setter
public class ProductImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "img_id") private Long id;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(name = "file_path") private String filePath;
    @Column(name = "sort_order") private Integer sortOrder = 0;
    @Column(name = "alt_text") private String altText;
}

