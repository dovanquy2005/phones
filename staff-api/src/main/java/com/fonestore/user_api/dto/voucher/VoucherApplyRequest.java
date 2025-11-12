package com.fonestore.user_api.dto.voucher;

import jakarta.validation.constraints.NotBlank;

public class VoucherApplyRequest {
    @NotBlank
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
