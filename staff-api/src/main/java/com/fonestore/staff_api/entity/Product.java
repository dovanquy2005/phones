package com.fonestore.staff_api.entity;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
@Entity @Table(name = "products")
@Getter @Setter
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id") private Long id;
    @Column(nullable = false) private String name;
    @Column private String slug;
    @Column(name = "brand_id") private Long brandId;
    @Column(name = "description", columnDefinition = "nvarchar(max)") private String description;
    @Column(name = "specs_json", columnDefinition = "nvarchar(max)") private String specsJson;
    @Column(name = "is_active") private Boolean isActive = true;
  
    @Column(nullable = false) private Integer quantity = 0;
}
