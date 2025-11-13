package com.fonestore.user_api.dto.checkout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutDTO {
    public record Item(String sku, String name, int quantity, long unit_price, long line_total) {}
    private List<Item> items;             // optional: may include for debugging
    private long subtotal;
    private long discount;
    private List<ShippingOption> shipping_options;
    private String selected_shipping;
    private long tax;
    private long total;
    private List<Warning> warnings;
    private List<String> payment_methods;
    // nested records
    @Data public static class ShippingOption { private String id; private String name; private long fee; private String eta; }
    @Data public static class Warning { private String type; private String sku; private Long old; private Long neo; private String reason; }
}
