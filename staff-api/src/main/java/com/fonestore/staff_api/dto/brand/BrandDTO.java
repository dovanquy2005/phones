package com.fonestore.staff_api.dto.brand;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class BrandDTO {
    private Long brandId;
    private String name;
}
