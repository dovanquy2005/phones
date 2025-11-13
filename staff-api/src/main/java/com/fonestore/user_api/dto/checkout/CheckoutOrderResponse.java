package com.fonestore.user_api.dto.checkout;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CheckoutOrderResponse  {
    private boolean ok;
    private String order_id;
    private int statusCode; // use to map to HTTP if needed
    private String reason;
    private List<CheckoutDTO.Warning> warnings;

    public static CheckoutOrderResponse success(String orderId) {
        CheckoutOrderResponse r = new CheckoutOrderResponse();
        r.ok = true; r.order_id = orderId; r.statusCode = 201; return r;
    }
    public static CheckoutOrderResponse conflict(String reason, List<CheckoutDTO.Warning> warnings) {
        CheckoutOrderResponse r = new CheckoutOrderResponse(); r.ok = false; r.statusCode = 409; r.reason = reason; r.warnings = warnings; return r;
    }
}
