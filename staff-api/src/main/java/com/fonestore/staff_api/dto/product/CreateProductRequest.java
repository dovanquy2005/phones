package com.fonestore.staff_api.dto.product;
import lombok.Data;
@Data
public class CreateProductRequest {
    private String name;

    private Long brandId;
    private String description;
    private String specsJson;
    private Boolean isActive;
    private String imagePath;
    private String firstColor;
    private String firstCapacity;
    private Long firstListPrice;
    private Integer quantity;
}
