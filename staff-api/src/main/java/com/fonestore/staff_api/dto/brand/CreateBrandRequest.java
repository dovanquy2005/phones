package com.fonestore.staff_api.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBrandRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
}
