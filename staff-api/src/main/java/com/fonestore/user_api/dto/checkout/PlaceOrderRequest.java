package com.fonestore.user_api.dto.checkout;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {
    @NotNull private String payment_method;
    // shipping address
    private ShippingAddress shipping_address;
    @Data public static class ShippingAddress { private String name; private String phone; private String email; private String address; }
}
