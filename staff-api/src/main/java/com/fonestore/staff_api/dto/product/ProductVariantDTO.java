package com.fonestore.staff_api.dto.product;
import lombok.*;
@Data @AllArgsConstructor @NoArgsConstructor
public class ProductVariantDTO {
    private Long id;
    private String color;
    private String capacity;
    private Long listPrice;
}
