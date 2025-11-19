package com.fonestore.staff_api.dto.product;
import lombok.*;
@Data @AllArgsConstructor @NoArgsConstructor
public class ProductListDTO {
    private Long productId;
    private Long brandId;
    private String name;
    private String brandName;
    private Long minPrice;
    private Boolean isActive;
    private String imagePath;
    private Integer quantity;
}
