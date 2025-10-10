package com.fonestore.staff_api.dto;
import lombok.Data;
@Data
public class UpdateProductRequest {
    private String name;
    private String slug;
    private Long brandId;
    private String description;
    private String specsJson;
    private Boolean isActive;
    private String imagePath;
    private String addColor;
    private String addCapacity;
    private Long addListPrice;
    private Integer quantity;
}
