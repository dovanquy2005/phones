package com.fonestore.staff_api.dto.product;
import lombok.*; import java.util.List;
@Data @AllArgsConstructor @NoArgsConstructor
public class ProductDetailDTO {
    private Long id;
    private String name;
    private String slug;
    private Long brandId;
    private String description;
    private String specsJson;
    private Boolean isActive;
    private String imagePath;
    private Integer quantity;
    private List<ProductVariantDTO> variants;
    private List<ProductImageDTO> images;
}
