package com.fonestore.staff_api.dto;
import lombok.*;
@Data @AllArgsConstructor @NoArgsConstructor
public class ProductImageDTO {
    private Long id;
    private String filePath;
    private Integer sortOrder;
}
