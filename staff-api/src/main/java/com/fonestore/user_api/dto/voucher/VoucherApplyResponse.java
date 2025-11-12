package com.fonestore.user_api.dto.voucher;

import java.math.BigDecimal;

public class VoucherApplyResponse {
    private boolean ok;
    private String type; // "fixed" or "percent"
    private BigDecimal discount; // cents OR VND amount depending on your convention; be consistent
    private Integer discountPercent;
    private String message;
    private String code; // echoed code

    public VoucherApplyResponse() {}

    public static VoucherApplyResponse okFixed(String code, BigDecimal discount, String message) {
        VoucherApplyResponse r = new VoucherApplyResponse();
        r.ok = true; r.code = code; r.type = "fixed"; r.discount = discount; r.message = message;
        return r;
    }
    public static VoucherApplyResponse okPercent(String code, Integer pct, String message) {
        VoucherApplyResponse r = new VoucherApplyResponse();
        r.ok = true; r.code = code; r.type = "percent"; r.discountPercent = pct; r.message = message;
        return r;
    }
    public static VoucherApplyResponse error(String message) {
        VoucherApplyResponse r = new VoucherApplyResponse();
        r.ok = false; r.message = message;
        return r;
    }

    // getters / setters
    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
